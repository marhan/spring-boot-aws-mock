package com.github.jojoldu.sqs.annotation.server;

import com.github.jojoldu.sqs.exception.SqsMockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.github.jojoldu.sqs.annotation.server.MockServerConstant.SQS_SERVER_PORT;

/**
 * Created by jojoldu@gmail.com on 2018. 5. 4.
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

@Slf4j
public abstract class OnMockSqsServerBaseCondition extends SpringBootCondition {

    boolean isRunning(ConditionContext context){
        String port = getSqsServerPort(context);
        if("random".equals(port)){
            return false;
        }
        return isRunning(port);
    }

    String getSqsServerPort(ConditionContext context) {
        Environment environment = context.getEnvironment();
        return environment.getProperty(SQS_SERVER_PORT);
    }

    boolean isRunning(String port) {
        verifyOS();  // check OS

        String line;
        StringBuilder pidInfo = new StringBuilder();
        Process p = executeGrepProcessCommand(port);

        try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {

            while ((line = input.readLine()) != null) {
                pidInfo.append(line);
            }

        } catch (Exception e) {
            String message = "isRunning Check Exception";
            log.error(message , e);
            throw new SqsMockException(message, e);
        }

        return !StringUtils.isEmpty(pidInfo.toString());
    }

    private Process executeGrepProcessCommand(String port) {
        String command = String.format("netstat -nat | grep LISTEN|grep %s", port);
        String[] shell = {"/bin/sh", "-c", command};
        try {
            return Runtime.getRuntime().exec(shell);
        } catch (IOException e) {
            String message = String.format("Execute Command Fail. command: %s",command);
            log.error(message , e);
            throw new SqsMockException(message, e);
        }
    }

    private void verifyOS() {
        if(isWindowsOS()){
            String message = "Not available on Windows OS";
            log.error(message);
            throw new SqsMockException(message);
        }
    }

    boolean isWindowsOS(){
        String os = System.getProperty("os.name");
        return os.contains("Windows");
    }

    String createMessage(boolean isRunning) {
        MockServerMessageType messageType = isRunning ? MockServerMessageType.USE_SERVER : MockServerMessageType.CREATE_SERVER;
        return messageType.getMessage();
    }
}