package com.dehnes.battery_capacity_tester

import mu.KotlinLogging
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit


interface SCPIChannel {
    fun addListener(id: String, onMsg: (msg: String) -> Unit)
    fun removeListener(id: String)
    fun send(msg: String)
    fun rpc(request: String, waitForResponses: Int = 1): List<String>
}


class UdpSCPIChannel(
    private val remoteAddr: InetAddress,
    private val remotePort: Int,
    private val localPort: Int,
    private val executionService: ExecutorService,
    private val rpcRetryCount: Int = 10
) : SCPIChannel {

    private val logger = KotlinLogging.logger { }
    private val listeners = ConcurrentHashMap<String, (msg: String) -> Unit>()

    @Volatile
    private var stop = true

    fun start() {
        stop = false
        executionService.submit {
            try {
                listenLoop()
            } catch (t: Throwable) {
                logger.error(t) { "Error" }
            }
        }
    }

    fun stop() {
        stop = true
    }

    private fun listenLoop() {
        val buf = ByteArray(1500)
        DatagramSocket(localPort).use { socket ->
            socket.soTimeout = 100

            while (!stop) {

                val packet = DatagramPacket(buf, buf.size)
                try {
                    socket.receive(packet)
                } catch (timeout: SocketTimeoutException) {
                    continue
                }

                if (!(packet.address == remoteAddr && packet.port == remotePort)) {
                    logger.warn { "Ignoring packet from ${packet.address} ${packet.port}" }
                    continue
                }

                val msg = String(buf, 0, packet.length).trim()
                logger.debug { "Received: $msg" }

                listeners.forEach { it.value(msg) }
            }
        }
    }


    override fun addListener(id: String, onMsg: (msg: String) -> Unit) {
        listeners[id] = onMsg
    }

    override fun removeListener(id: String) {
        listeners.remove(id)
    }

    override fun send(msg: String) {
        DatagramSocket().use { s ->
            logger.debug { "Sending: $msg" }
            val buf = (msg + "\n").toByteArray()
            s.send(
                DatagramPacket(
                    buf,
                    0,
                    buf.size,
                    remoteAddr,
                    remotePort
                )
            )
        }
    }

    override fun rpc(request: String, waitForResponses: Int): List<String> {
        repeat(rpcRetryCount) {
            try {
                return rpcOnce(request, waitForResponses)
            } catch (e: Exception) {
                // try again
            }
        }

        error("Timeout")
    }

    private fun rpcOnce(request: String, waitForResponses: Int): List<String> {
        val rpcId = UUID.randomUUID().toString()
        val countDownLatch = CountDownLatch(waitForResponses)
        val responses = LinkedList<String>()
        addListener(rpcId) {
            synchronized(responses) {
                responses.add(it)
            }
            countDownLatch.countDown()
        }

        try {
            send(request)
            check(countDownLatch.await(500, TimeUnit.MILLISECONDS))

            return synchronized(responses) {
                responses
            }
        } finally {
            removeListener(rpcId)
        }
    }
}