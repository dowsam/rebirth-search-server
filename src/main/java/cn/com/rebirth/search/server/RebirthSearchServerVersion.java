/*
 * Copyright (c) 2005-2012 www.summall.com.cn All rights reserved
 * Info:summall-search-server VersionImpl.java 2012-3-30 9:08:36 l.xue.nong$$
 */
package cn.com.rebirth.search.server;

import cn.com.rebirth.commons.Version;

/**
 * The Class VersionImpl.
 *
 * @author l.xue.nong
 */
public class RebirthSearchServerVersion implements Version {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -3140641643166753463L;

	@Override
	public String getModuleVersion() {
		return "0.0.1.RC1-SNAPSHOT";
	}

	@Override
	public String getModuleName() {
		return "rebirth-search-server";
	}

}
