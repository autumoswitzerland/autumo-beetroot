package ch.autumo.beetroot.security.password;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ch.autumo.beetroot.BeetRootConfigurationManager;

public class PasswordHashTest {

	@Test
	public void test() throws Exception {
		
		long start = -1;
		long end = -1;
		
		Argon2HashProvider a = new Argon2HashProvider();
		
		start = System.currentTimeMillis();
		
		String h = a.hash("ifacex");

		end = System.currentTimeMillis();
		System.out.println("Argon2 Time (ms): " + (end-start));		

		System.err.println("Argon2 hash: " + h);		
		
		assertTrue(a.verify("ifacex", h));
		assertTrue(a.verify("ifacex", "$argon2id$v=19$m=65536,t=3,p=2$SLS4B4++8yzR00TJAoqezQ$fO3yb+JquGyEiL3ZSXmiLyXII6t2B1api19J8v8BDIQ"));
		assertTrue(a.verify("ifacex", "$argon2id$v=19$m=65536,t=3,p=2$LuA8LbY9xKici7GCO6DfJA$2Kikx3sIADB8aP9gqstMssY1D5rNyt9Mm50LwWx1/X4"));


		// needed for seed
		BeetRootConfigurationManager.getInstance().initialize("cfg/beetroot_test.cfg");
		
		PBKPD2HashProvider p = new PBKPD2HashProvider();
		
		start = System.currentTimeMillis();
		
		h = p.hash("ifacex");
		
		end = System.currentTimeMillis();
		System.out.println("PBKPD2 Time (ms): " + (end-start));		
		
		System.err.println("PBKPD2 hash: " + h);		
		
		assertTrue(p.verify("ifacex", h));
		assertTrue(p.verify("ifacex", "f1b42d6e77b788cd196df86b20192028be894926bcb8d05cfbe159e2c98746064efbc033b258310af76cf5ff2bb48864"));
		assertTrue(p.verify("ifacex", "30e8cee49d40c555a0fc5099d454a781ff64ad8e5a7f08221b1aa6648fbf05cf754b3530e7fd4a6535e7569de7ce1d67"));
	}

}
