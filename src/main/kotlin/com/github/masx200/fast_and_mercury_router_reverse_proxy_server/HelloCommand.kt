package com.github.masx200.fast_and_mercury_router_reverse_proxy_server

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

/**
 * 创建一个命令行接口命令类，用于处理特定的命令行交互。
 * 该类继承自CliktCommand，利用Clikt库简化命令行接口的创建过程。
 * 它允许通过名称初始化命令，并定义如何处理命令行参数和选项。
 *
 * @param name 命令的名称，用于在命令行中标识该命令。
 * @param callback 当命令执行时调用的回调函数，它接收一个HelloCommand实例作为参数。
 */
class HelloCommand(name: String, val callback: (options: HelloCommand) -> Unit) : CliktCommand(name = name) {
    /**
     * 执行命令的操作。
     * 当该命令被调用时，此方法会被执行，它将调用初始化时提供的回调函数，
     * 并将当前命令实例作为参数传递给回调函数。
     */
    override fun run() {
        callback(this)
    }

    /**
     * 重写toString方法，提供命令的字符串表示。
     * 这对于调试和日志记录特别有用，因为它包含了命令的关键信息，
     * 比如upstream和port，以及基类CliktCommand的信息。
     *
     * @return 返回包含命令关键信息的字符串。
     */
    override fun toString(): String {
        return "HelloCommand(upstream='$upstream', port='$port')" + super.toString()
    }

    /**
     * 定义upstream属性，通过命令行选项参数获取其值。
     * 该属性是必须的，用户在命令行中使用-u或--upstream选项来设置该值。
     * 它代表了命令需要连接的上游服务器地址。
     */
    val upstream: String by option("-u", "--upstream", help = "upstream").required()

    /**
     * 定义port属性，通过命令行选项参数获取其值。
     * 该属性是必须的，用户在命令行中使用-p或--port选项来设置该值。
     * 它代表了命令需要连接的端口号。
     */
    val port: String by option("-p", "--port", help = "port").required()
}