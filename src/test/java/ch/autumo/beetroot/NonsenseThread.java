package ch.autumo.beetroot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NonsenseThread {

	protected static final Logger LOG = LoggerFactory.getLogger(NonsenseThread.class.getName());
	public static void nonsenseThread() {
		final Thread thread = new Thread(() -> {
			try {
	            while (true) {
	            	LOG.info("Log some nonsense!");
	                Thread.sleep(1000);
	            }
	        } catch (InterruptedException e) {
	            System.err.println("Nonsense thread was interrupted!");
	        }				
	    });
		thread.setDaemon(true);
	    thread.start();
	}
}
