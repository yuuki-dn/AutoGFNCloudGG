import org.junit.*;

import dev.hachikuu.autogfncloudgg.AppThread;

public class AppTest {
    public AppThread thread;
    @Before
    public void before() {
        thread = new AppThread();
        thread.init();
    }

    @Test
    public void test1() {
        thread.quit();
    }


    @Test
    public void run() {
        thread.run();
    }

}
