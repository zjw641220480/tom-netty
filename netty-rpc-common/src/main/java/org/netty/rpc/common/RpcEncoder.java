package org.netty.rpc.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 
 * <p>Titile:RpcEncoder</p>
 * <p>Description: RPC编码器</p>
 * @author TOM
 * @date 2017年5月14日 下午2:53:56
 */
public class RpcEncoder extends MessageToByteEncoder {

	private Class<?> genericClass;

	// 构造函数传入向反序列化的class
	public RpcEncoder(Class<?> genericClass) {
		this.genericClass = genericClass;
	}

	@Override
	public void encode(ChannelHandlerContext ctx, Object inob, ByteBuf out)
			throws Exception {
		//序列化
		if (genericClass.isInstance(inob)) {
			byte[] data = SerializationUtil.serialize(inob);
			out.writeInt(data.length);
			out.writeBytes(data);
		}
	}
}