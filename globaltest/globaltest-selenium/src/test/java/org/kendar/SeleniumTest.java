package org.kendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kendar.globaltest.ProcessUtils;

import java.util.HashMap;

// [\t ]+(driver.findElement)([\(a-zA-Z0-9\.\":\-\ )]+)(.click\(\))
// doClick(()->$1$1)
@ExtendWith({SeleniumBase.class})
public class SeleniumTest extends SeleniumBase {

    ProcessUtils _processUtils = new ProcessUtils(new HashMap<>());

    @Test
    void simpleTest(){
        System.out.println("simple");
    }
    @Test
    void googleHack() throws Throwable {
            beforeAll(null);
            runHamJar(SeleniumTest.class);
            var driver = SeleniumBase.getDriver();
            GoogleHackSetupTest.setup(driver);
            restart();
            driver = SeleniumBase.getDriver();
            GoogleHackSetupTest.verify(driver);
            close();
    }

    @Test
    void dbRecording() throws Throwable {
        beforeAll(null);
        runHamJar(SeleniumTest.class);
        var driver = SeleniumBase.getDriver();
        DbRecordingSetupTest.startup(driver);


        //Create recording
        String mainId = DbRecordingSetupTest.startRecording(driver, "Main");
        DbRecordingUiActions.fullNavigation(driver);
        DbRecordingSetupTest.stopAction(driver, mainId);
        //TODODbRecordingSetupTest.analyzeRecording(driver,mainId);

        //Do Ui test
        String uiTestId = DbRecordingPrepareTest.cloneTo(driver, mainId, "UiTest");
        DbRecordingPrepareTest.prepareUiTest(driver, uiTestId);
        DbRecordingSetupTest.startPlaying(driver, uiTestId);
        DbRecordingUiActions.fullNavigation(driver);
        DbRecordingSetupTest.stopAction(driver, uiTestId);

        //Do Gateway null test
        String gatewayTestId = DbRecordingPrepareTest.cloneTo(driver, mainId, "GatewayNullTest");
        DbRecordingPrepareTest.prepareGatewayNullTest(driver, gatewayTestId);
        DbRecordingSetupTest.startNullPlaying(driver, gatewayTestId);
        DbRecordingSetupTest.loadResults(driver, gatewayTestId);

        //Do Be fake db test
        String dbNullTest = DbRecordingPrepareTest.cloneTo(driver, mainId, "DbNullTest");
        DbRecordingPrepareTest.prepareDbNullTest(driver, dbNullTest);
        DbRecordingSetupTest.initializeNullPlayingDb(driver, dbNullTest);
        DbRecordingSetupTest.startNullPlaying(driver, dbNullTest);
        DbRecordingSetupTest.loadResults(driver, dbNullTest);

        close();
    }
}
