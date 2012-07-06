/*
 * Copyright (c) 2005-2012 www.summall.com.cn All rights reserved
 * Info:summall-search-server ElasticSearchF.java 2012-3-29 17:43:33 l.xue.nong$$
 */
package cn.com.rebirth.search.server.bootstrap;

import java.io.IOException;

/**
 * The Class ElasticSearchF.
 *
 * @author l.xue.nong
 */
public class RebirthSearchF {

	/**
	 * Close.
	 *
	 * @param args the args
	 */
	public static void close(String[] args) {
		Bootstrap.close(args);
	}

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		System.setProperty("es.foreground", "yes");
		Bootstrap.main(args);
	}
}
