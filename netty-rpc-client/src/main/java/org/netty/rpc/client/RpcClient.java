package org.netty.rpc.client;

import org.netty.rpc.common.RpcDecoder;
import org.netty.rpc.common.RpcEncoder;
import org.netty.rpc.common.RpcRequest;
import org.netty.rpc.common.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * 
 * <p>Titile:RpcClient</p>
 * <p>Description:框架的RPC客户端（用于发送 RPC 请求）,链接服务器并发送信息 ,此类在RPCProxy中被调用</p>
 * @author TOM
 * @date 2017年5月14日 下午2:50:28
 */
// TODO 这里后面可以把继承写成一个内部类(这样理解起来会更好一些);
public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(RpcClient.class);

	private String host;
	private int port;

	private RpcResponse response;

	private final Object obj = new Object();

	public RpcClient(String host, int port) {
		this.host = host;
		this.port = port;
	}

	/**
	 * 
	 * @MethodName:send
	 * @Description:链接服务端，发送消息
	 * @param request
	 * @return
	 * @throws Exception
	 * @Time: 2017年5月14日 下午2:51:08
	 * @author: TOM
	 */
	public RpcResponse send(RpcRequest request) throws Exception {
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(group).channel(NioSocketChannel.class)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel channel)
								throws Exception {
							// 向pipeline中添加编码、解码、业务处理的handler
							channel.pipeline()
									.addLast(new RpcEncoder(RpcRequest.class))  //OUT - 1
									.addLast(new RpcDecoder(RpcResponse.class)) //IN - 1
									.addLast(RpcClient.this);                   //IN - 2
						}
					}).option(ChannelOption.SO_KEEPALIVE, true);
			// 链接服务器
			ChannelFuture future = bootstrap.connect(host, port).sync();
			//将request对象写入outbundle处理后发出（即RpcEncoder编码器）
			future.channel().writeAndFlush(request).sync();

			// 用线程等待的方式决定是否关闭连接
			// 其意义是：先在此阻塞，等待获取到服务端的返回后，被唤醒，从而关闭网络连接
			synchronized (obj) {
				obj.wait();
			}
			if (response != null) {
				future.channel().closeFuture().sync();
			}
			return response;
		} finally {
			group.shutdownGracefully();
		}
	}

	/**
	 * 读取服务端的返回结果
	 */
	@Override
	public void channelRead0(ChannelHandlerContext ctx, RpcResponse response)
			throws Exception {
		this.response = response;

		synchronized (obj) {
			obj.notifyAll();
		}
	}

	/**
	 * 异常处理
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		LOGGER.error("client caught exception", cause);
		ctx.close();
	}

	
}
