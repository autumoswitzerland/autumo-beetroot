package ch.autumo.beetroot.general;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.utils.database.H2Url;

public class H2UrlTest {

	//protected static final Logger LOG = LoggerFactory.getLogger(H2UrlTest.class.getName());
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BeetRootConfigurationManager.getInstance().initialize("cfg/beetroot_test.cfg");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Exception {
		new H2Url("jdbc:h2:/Users/Mike/Downloads/test/test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=SECOND,MINUTE,DAY,MONTH,YEAR").getUrl();
		new H2Url("jdbc:h2:/Users/Mike/Downloads/test/test;DATABASE_TO_LOWER=TRUE").getUrl();
		new H2Url("jdbc:h2:/Users/Mike/Downloads/test/test").getUrl();
	}

	/**
	@Test
	public void testLog() throws Exception {
		LOG.error("Couldn't load property '{}' from database!", "var" , new Exception("Test ex!"));
	}
	*/
	
}
