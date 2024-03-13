import dev.hachikuu.autogfncloudgg.utils.MailProvider;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class MailProviderTest {
    private MailProvider mail;

    @Before
    public void setUp() {
        // Khởi tạo đối tượng MailProvider và thiết lập thuộc tính mailPrefix trước mỗi phương thức kiểm thử
        MailProvider.mailPrefix = "lumine_";
        mail = new MailProvider();
    }

    @Test
    public void testGenerateEmail() {
        // Kiểm tra xem phương thức generateEmail() trả về chuỗi có phải là một địa chỉ email hợp lệ không
        String email = mail.generateEmail();
        // Bạn có thể thay đổi điều kiện kiểm tra tùy thuộc vào logic của phương thức generateEmail()
        assertEquals(true, isValidEmail(email));
    }

    @Test
    public void testGetActivationLink() {
        // Kiểm tra xem phương thức getActivationLink() trả về chuỗi có chứa "lumine_" không
        String activationLink = mail.getActivationLink();
        // Bạn có thể thay đổi điều kiện kiểm tra tùy thuộc vào logic của phương thức getActivationLink()
        assertEquals(true, activationLink.contains("lumine_"));
    }

    // Phương thức hỗ trợ kiểm tra chuỗi có phải là một địa chỉ email hợp lệ không
    private boolean isValidEmail(String email) {
        // Bạn có thể thay đổi logic kiểm tra tùy thuộc vào quy định của địa chỉ email
        return email != null && email.contains("@");
    }
}