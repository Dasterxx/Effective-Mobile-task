package nikita.com.bankcards;

import nikita.com.bankcards.service.auth.AuthServiceImplTest;
import nikita.com.bankcards.service.card.CardServiceAdminTest;
import nikita.com.bankcards.service.transfer.TransferServiceFullTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Critical Path Tests")
@SelectClasses({
        AuthServiceImplTest.class,
        CardServiceAdminTest.class,
        TransferServiceFullTest.class
})
public class CriticalPathTestSuite {}
