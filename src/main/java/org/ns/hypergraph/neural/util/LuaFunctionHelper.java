package org.ns.hypergraph.neural.util;

import java.util.Map;

import com.naef.jnlua.LuaState;
import com.sun.jna.Pointer;

import gnu.trove.list.TIntList;
import th4j.Tensor;
import th4j.Tensor.DoubleTensor;

public class LuaFunctionHelper {
	public static Object[] execLuaFunction(LuaState L, String functionName, Object[] inputs, Class<?>[] outputTypes) {
		L.getGlobal(functionName);
		
		// prepare inputs
		for (Object obj : inputs) {
			if (obj instanceof String) {
				L.pushString((String) obj);
			} else if (obj instanceof Double) {
				L.pushNumber((double) obj);
			} else if (obj instanceof Boolean) {
				L.pushBoolean((boolean) obj);
			} else if (obj instanceof double[][]) {
				@SuppressWarnings("rawtypes")
				Tensor t = new DoubleTensor((double[][]) obj);
				L.pushNumber(t.getPeerPtr());
			} else if (obj instanceof DoubleTensor) {
				L.pushNumber(((DoubleTensor) obj).getPeerPtr());
			} else if (obj instanceof Map) {
				L.pushJavaObject(obj);
			} else if (obj instanceof TIntList) {
				L.pushJavaObject(obj);
			} else {
				throw new RuntimeException("unknown or unadded types: "+obj.getClass());
			}
		}
		
		// call the function
		L.call(inputs.length, outputTypes.length);
		
		// get return values
		Object[] outputs = new Object[outputTypes.length];
		int i = 1;
		for (Class<?> cls : outputTypes) {
			if (cls.equals(String.class)) {
				outputs[i-1] = L.toString(i);
			} else if (cls.equals(Double.class)) {
				outputs[i-1] = L.toNumber(i);
			} else if (cls.equals(DoubleTensor.class)) {
				Pointer ptr = new Pointer((long)L.toNumber(i));
				outputs[i-1] = new DoubleTensor(ptr);
			} else {
				throw new RuntimeException("unknown or unadded types."+cls);
			}
			i++;
		}
		L.pop(outputTypes.length);
		return outputs;
	}
}
