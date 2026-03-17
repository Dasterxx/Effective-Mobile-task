package nikita.com.bankcards;

import nikita.com.bankcards.service.auth.AuthServiceImplTest;
import nikita.com.bankcards.service.card.CardServiceAdminTest;
import nikita.com.bankcards.service.encryption.EncryptionServiceImplTest;
import nikita.com.bankcards.service.transfer.TransferServiceFullTest;
import nikita.com.bankcards.service.user.CardServiceUserTest;
import nikita.com.bankcards.service.user.UserServiceImplTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("All Services Test Suite - Ordered Actions")
@SelectClasses({
        AuthServiceImplTest.class,
        UserServiceImplTest.class,
        CardServiceAdminTest.class,
        CardServiceUserTest.class,
        TransferServiceFullTest.class,
        EncryptionServiceImplTest.class
})
public class AllServicesTestSuite {
}
