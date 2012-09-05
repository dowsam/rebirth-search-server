/*
 * Copyright (c) 2005-2012 www.china-cti.com All rights reserved
 * Info:rebirth-search-server RebirthSearchServerVersion.java 2012-7-19 13:14:18 l.xue.nong$$
 */
package cn.com.rebirth.search.server;

import cn.com.rebirth.commons.AbstractVersion;
import cn.com.rebirth.commons.Version;

/**
 * The Class RebirthSearchServerVersion.
 *
 * @author l.xue.nong
 */
public class RebirthSearchServerVersion extends AbstractVersion implements Version {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -3140641643166753463L;

	/* (non-Javadoc)
	 * @see cn.com.rebirth.commons.Version#getModuleName()
	 */
	@Override
	public String getModuleName() {
		return "rebirth-search-server";
	}

}
