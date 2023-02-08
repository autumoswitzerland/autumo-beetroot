package ch.autumo.beetroot.security.password;

import org.junit.Test;

import ch.autumo.beetroot.BeetRootConfigurationManager;

public class PasswordHashTest {

	@Test
	public void test() throws Exception {
		Argon2HashProvider a = new Argon2HashProvider();
		
		long start = System.currentTimeMillis();
		
		String h = a.hash("ifacex");
		System.err.println(h);
		
		
		System.err.println("VERIFY 1: " +a.verify("ifacex", "$argon2id$v=19$m=65536,t=3,p=2$SLS4B4++8yzR00TJAoqezQ$fO3yb+JquGyEiL3ZSXmiLyXII6t2B1api19J8v8BDIQ"));
		System.err.println("VERIFY 2: " +a.verify("ifacex", "$argon2id$v=19$m=65536,t=3,p=2$LuA8LbY9xKici7GCO6DfJA$2Kikx3sIADB8aP9gqstMssY1D5rNyt9Mm50LwWx1/X4"));
		
		long end = System.currentTimeMillis();
		
		System.err.println("Time (ms): " + (end-start));		
		
		
		BeetRootConfigurationManager.getInstance().initialize("cfg/beetroot.cfg");
		
		PBKPD2HashProvider p = new PBKPD2HashProvider();
		
		start = System.currentTimeMillis();
		
		h = p.hash("ifacex");
		System.err.println(h);
		System.err.println(p.verify("ifacex", h));
		
		end = System.currentTimeMillis();
		
		System.err.println("Time (ms): " + (end-start));		
	}

}
