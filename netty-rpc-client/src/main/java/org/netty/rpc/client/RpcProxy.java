package org.netty.rpc.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

import org.netty.rpc.common.RpcRequest;
import org.netty.rpc.common.RpcResponse;
import org.netty.rpc.registry.ServiceDiscovery;

/**
 * 
 * <p>Titile:RpcProxy</p>
 * <p>Description: RPC 代理（用于创建 RPC 服务代理）,创建代理对象,并调用RPCClient,从而将信息发送到服务端</p>
 * @author TOM
 * @date 2017年5月14日 下午2:46:49
 */
public class RpcProxy {

	private String serverAddress;
	//从Zookeeper中根据负载均衡获取到对应服务
	private ServiceDiscovery serviceDiscovery;

	public RpcProxy(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public RpcProxy(ServiceDiscovery serviceDiscovery) {
		this.serviceDiscovery = serviceDiscovery;
	}

	/**
	 * 
	 * @MethodName:create
	 * @Description:根据传递过来的类的class对象获取其代理对象,在代理类中调用invoke方法,进而实现数据的传递
	 * @param interfaceClass
	 * @return
	 * @Time: 2017年5月14日 下午2:48:58
	 * @author: TOM
	 */
	@SuppressWarnings("unchecked")
	public <T> T create(Class<?> interfaceClass) {
		//使用JDK动态代理获取传递过来的类的代理对象
		return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
				new Class<?>[] { interfaceClass }, new InvocationHandler() {
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						//创建RpcRequest，封装被代理类的属性
						RpcRequest request = new RpcRequest();
						request.setRequestId(UUID.randomUUID().toString());
						//拿到声明这个方法的业务接口名称
						request.setClassName(method.getDeclaringClass()
								.getName());
						request.setMethodName(method.getName());
						request.setParameterTypes(method.getParameterTypes());
						request.setParameters(args);
						//查找服务
						if (serviceDiscovery != null) {
							serverAddress = serviceDiscovery.discover();
						}
						//随机获取服务的地址
						String[] array = serverAddress.split(":");
						String host = array[0];
						int port = Integer.parseInt(array[1]);
						//创建Netty实现的RpcClient，链接服务端
						RpcClient client = new RpcClient(host, port);
						//通过netty向服务端发送请求
						RpcResponse response = client.send(request);
						//返回信息
						if (response.isError()) {
							throw response.getError();
						} else {
							return response.getResult();
						}
					}
				});
	}
}
