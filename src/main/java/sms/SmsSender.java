package sms;

import exception.ShortMessageException;
import listener.SMSReceiverListener;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.*;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.extra.SessionState;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SmsSender implements Sender {
    private static final Logger LOG = LoggerFactory.getLogger(SmsSender.class);
    private SMPPSession session;

    @Value("$smpp.server")
    private String server;
    @Value("$smpp.port")
    private int port;
    @Value("$smpp.systemId")
    private String systemId;
    @Value("$smpp.password")
    private String password;

    public SmsSender(Properties properties) {
        sessionConfiguration();
    }

    @Override
    public void send(MessageData messageData) throws Exception {
        Map<String, String> data = prepareData(messageData);
        String message = data.get("message");
        String phone = data.get("to");
        String shortNumber = data.get("from");
        if (!session.getSessionState().equals(SessionState.BOUND_TRX)) {
            session.unbindAndClose();
            sessionConfiguration();
        }

        ///////////////////////////////
        // Config data
        ///////////////////////////////
        try {
            final RegisteredDelivery registeredDelivery = new RegisteredDelivery();
            registeredDelivery.setSMSCDeliveryReceipt(SMSCDeliveryReceipt.SUCCESS_FAILURE);

            String messageId = session.submitShortMessage("CMT",
                    TypeOfNumber.ALPHANUMERIC, NumberingPlanIndicator.UNKNOWN, shortNumber,
                    TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.ISDN, phone,
                    new ESMClass(), (byte) 0, (byte) 1, null, null, registeredDelivery, (byte) 0,
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT, MessageClass.CLASS1, false),
                    (byte) 0, message.getBytes());
            LOG.debug("Message submitted, message_id is " + messageId);
        } catch (PDUException e) {
            LOG.debug("Invalid PDU parameter: " + e.getMessage());
            throw new ShortMessageException("Invalid PDU parameter");
        } catch (ResponseTimeoutException e) {
            // Response timeout
            LOG.debug("Response timeout: " + e.getMessage());
            throw new ShortMessageException("Response has been timedout");
        } catch (InvalidResponseException e) {
            // Invalid response
            LOG.debug("Receive invalid response: " + e.getMessage());
            throw new ShortMessageException("Invalid response has been Receive");
        } catch (NegativeResponseException e) {
            // Receiving negative response (non-zero command_status)
            LOG.debug("Receive negative response: " + e.getMessage());
            throw new ShortMessageException("non-zero command-status");
        } catch (IOException e) {
            LOG.debug("IO error occur: " + e.getMessage());
            throw new ShortMessageException("IO error occurred");
        }
        // wait 3 second
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            LOG.debug("Interruption exception " + e.getMessage());
            throw new ShortMessageException("Interruption has been occurred while sending short message");
        }
        // unbind(disconnect)
        LOG.debug("finish!");

    }

    private Map<String, String> prepareData(MessageData messageData) {
        Map<String, String> data = new HashMap<>();

        String to = messageData.to;
        if (to.startsWith("00")) {
            to = to.substring(2);
        }

        data.put("to", to);
        data.put("from", messageData.from);

        String msg = messageData.body;
        if (msg.length() > 250) {
            msg = msg.substring(0, 250);
        }
        data.put("message", msg);
        return data;
    }

    private void sessionConfiguration() {
        session = new SMPPSession();
        session.setMessageReceiverListener(new SMSReceiverListener(/*this.messageLogRepository*/));
        try {
            LOG.info("Trying to connect to SMPP server: " + server + " ...");
            session.connectAndBind(server, port, new BindParameter(BindType.BIND_TRX, systemId, password, "cp",
                    TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, null));
        } catch (IOException e) {
            LOG.debug("Failed to connect and bind to host: " + e.getMessage());
        }
    }
}
