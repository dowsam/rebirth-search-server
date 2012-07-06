/*
 * Copyright (c) 2005-2012 www.summall.com.cn All rights reserved
 * Info:summall-search-server Bootstrap.java 2012-3-29 17:43:21 l.xue.nong$$
 */

package cn.com.rebirth.search.server.bootstrap;

import static com.google.common.collect.Sets.newHashSet;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.com.rebirth.commons.RebirthContainer;
import cn.com.rebirth.commons.collect.Tuple;
import cn.com.rebirth.commons.exception.ExceptionsHelper;
import cn.com.rebirth.commons.jna.Natives;
import cn.com.rebirth.commons.settings.Settings;
import cn.com.rebirth.search.commons.inject.CreationException;
import cn.com.rebirth.search.commons.inject.spi.Message;
import cn.com.rebirth.search.commons.io.FileSystemUtils;
import cn.com.rebirth.search.commons.settings.ImmutableSettings;
import cn.com.rebirth.search.core.env.Environment;
import cn.com.rebirth.search.core.jmx.JmxService;
import cn.com.rebirth.search.core.monitor.jvm.JvmInfo;
import cn.com.rebirth.search.core.node.Node;
import cn.com.rebirth.search.core.node.NodeBuilder;
import cn.com.rebirth.search.core.node.internal.InternalSettingsPerparer;
import cn.com.rebirth.search.server.RebirthSearchServerVersion;

/**
 * The Class Bootstrap.
 *
 * @author l.xue.nong
 */
public class Bootstrap {

	/** The logger. */
	private static Logger logger = LoggerFactory.getLogger(Bootstrap.class);

	/** The node. */
	private Node node;

	/** The keep alive thread. */
	private static volatile Thread keepAliveThread;

	/** The keep alive latch. */
	private static volatile CountDownLatch keepAliveLatch;

	/** The bootstrap. */
	private static Bootstrap bootstrap;

	/**
	 * Setup.
	 *
	 * @param addShutdownHook the add shutdown hook
	 * @param tuple the tuple
	 * @throws Exception the exception
	 */
	private void setup(boolean addShutdownHook, Tuple<Settings, Environment> tuple) throws Exception {
		if (tuple.v1().getAsBoolean("bootstrap.mlockall", false)) {
			Natives.tryMlockall();
		}
		tuple = setupJmx(tuple);

		NodeBuilder nodeBuilder = NodeBuilder.nodeBuilder().settings(tuple.v1()).loadConfigSettings(false);
		node = nodeBuilder.build();
		if (addShutdownHook) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					node.close();
				}
			});
		}
	}

	/**
	 * Setup jmx.
	 *
	 * @param tuple the tuple
	 * @return the tuple
	 */
	private static Tuple<Settings, Environment> setupJmx(Tuple<Settings, Environment> tuple) {
		if (tuple.v1().get(JmxService.SettingsConstants.CREATE_CONNECTOR) == null) {
			// automatically create the connector if we are bootstrapping
			Settings updated = ImmutableSettings.settingsBuilder().put(tuple.v1())
					.put(JmxService.SettingsConstants.CREATE_CONNECTOR, true).build();
			tuple = new Tuple<Settings, Environment>(updated, tuple.v2());
		}
		return tuple;
	}

	/**
	 * Initial settings.
	 *
	 * @return the tuple
	 */
	private static Tuple<Settings, Environment> initialSettings() {
		return InternalSettingsPerparer.prepareSettings(ImmutableSettings.Builder.EMPTY_SETTINGS, true);
	}

	/**
	 * Inits the.
	 *
	 * @param args the args
	 * @throws Exception the exception
	 */
	public void init(String[] args) throws Exception {
		Tuple<Settings, Environment> tuple = initialSettings();
		setup(true, tuple);
	}

	/**
	 * Start.
	 */
	public void start() {
		node.start();
	}

	/**
	 * Stop.
	 */
	public void stop() {
		node.stop();
	}

	/**
	 * Destroy.
	 */
	public void destroy() {
		node.close();
	}

	/**
	 * Close.
	 *
	 * @param args the args
	 */
	public static void close(String[] args) {
		bootstrap.destroy();
		keepAliveLatch.countDown();
		RebirthContainer.getInstance().stop();
	}

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		RebirthContainer.getInstance().start();
		bootstrap = new Bootstrap();
		final String pidFile = System.getProperty("es.pidfile", System.getProperty("es-pidfile"));
		if (pidFile != null) {
			try {
				File fPidFile = new File(pidFile);
				if (fPidFile.getParentFile() != null) {
					FileSystemUtils.mkdirs(fPidFile.getParentFile());
				}
				RandomAccessFile rafPidFile = new RandomAccessFile(fPidFile, "rw");
				rafPidFile.writeBytes(Long.toString(JvmInfo.jvmInfo().pid()));
				rafPidFile.close();

				fPidFile.deleteOnExit();
			} catch (Exception e) {
				String errorMessage = buildErrorMessage("pid", e);
				System.err.println(errorMessage);
				System.err.flush();
				System.exit(3);
			}
		}

		boolean foreground = System.getProperty("es.foreground", System.getProperty("es-foreground")) != null;
		// handle the wrapper system property, if its a service, don't run as a service
		if (System.getProperty("wrapper.service", "XXX").equalsIgnoreCase("true")) {
			foreground = false;
		}

		Tuple<Settings, Environment> tuple = null;
		try {
			tuple = initialSettings();
		} catch (Exception e) {
			String errorMessage = buildErrorMessage("Setup", e);
			System.err.println(errorMessage);
			System.err.flush();
			System.exit(3);
		}

		if (System.getProperty("es.max-open-files", "false").equals("true")) {
			logger.info("max_open_files [{}]",
					FileSystemUtils.maxOpenFiles(new File(tuple.v2().workFile(), "open_files")));
		}

		// warn if running using the client VM
		if (JvmInfo.jvmInfo().vmName().toLowerCase().contains("client")) {
			logger.warn("jvm uses the client vm, make sure to run `java` with the server vm for best performance by adding `-server` to the command line");
		}

		String stage = "Initialization";
		try {
			if (!foreground) {
				System.out.close();
			}
			bootstrap.setup(true, tuple);

			stage = "Startup";
			bootstrap.start();

			if (!foreground) {
				System.err.close();
			}

			keepAliveLatch = new CountDownLatch(1);
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					keepAliveLatch.countDown();
				}
			});

			keepAliveThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						keepAliveLatch.await();
					} catch (InterruptedException e) {
						// bail out
					}
				}
			}, "SumMallSearch[keepAlive]");
			keepAliveThread.setDaemon(false);
			keepAliveThread.start();
		} catch (Throwable e) {
			e.printStackTrace();
			String errorMessage = buildErrorMessage(stage, e);
			if (foreground) {
				logger.error(errorMessage);
			} else {
				System.err.println(errorMessage);
				System.err.flush();
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Exception", e);
			}
			System.exit(3);
		}
	}

	/**
	 * Builds the error message.
	 *
	 * @param stage the stage
	 * @param e the e
	 * @return the string
	 */
	private static String buildErrorMessage(String stage, Throwable e) {
		StringBuilder errorMessage = new StringBuilder("{").append(new RebirthSearchServerVersion().getModuleVersion())
				.append("}: ");
		errorMessage.append(stage).append(" Failed ...\n");
		if (e instanceof CreationException) {
			CreationException createException = (CreationException) e;
			Set<String> seenMessages = newHashSet();
			int counter = 1;
			for (Message message : createException.getErrorMessages()) {
				String detailedMessage;
				if (message.getCause() == null) {
					detailedMessage = message.getMessage();
				} else {
					detailedMessage = ExceptionsHelper.detailedMessage(message.getCause(), true, 0);
				}
				if (detailedMessage == null) {
					detailedMessage = message.getMessage();
				}
				if (seenMessages.contains(detailedMessage)) {
					continue;
				}
				seenMessages.add(detailedMessage);
				errorMessage.append("").append(counter++).append(") ").append(detailedMessage);
			}
		} else {
			errorMessage.append("- ").append(ExceptionsHelper.detailedMessage(e, true, 0));
		}
		return errorMessage.toString();
	}
}
