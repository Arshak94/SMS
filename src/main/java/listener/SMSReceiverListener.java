package listener;

import org.jsmpp.SMPPConstant;
import org.jsmpp.bean.AlertNotification;
import org.jsmpp.bean.DataSm;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.MessageType;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Date;

public class SMSReceiverListener implements MessageReceiverListener {
    private static final String DATASM_NOT_IMPLEMENTED = "data_sm not implemented";
    private static final Logger LOG = LoggerFactory.getLogger(SMSReceiverListener.class);
//    @Autowired
//    private MessageLogRepository messageLogRepository;

//    public SMSReceiverListener(MessageLogRepository messageLogRepository) {
//        this.messageLogRepository = messageLogRepository;
//    }

    @Override
    public void onAcceptDeliverSm(DeliverSm deliverSm) {
        byte[] data = deliverSm.getShortMessage();
        if (MessageType.SMSC_DEL_RECEIPT.containedIn(deliverSm.getEsmClass())) {
            try {
                prepareStringAndAddToRepo(data);
            } catch (Exception e) {
                LOG.debug("Receive failed");
            }
        } else {
            LOG.debug("Receiving message : " + new String(data));
        }
    }

    @Override
    public void onAcceptAlertNotification(AlertNotification alertNotification) {
        LOG.info("onAcceptAlertNotification");
    }

    @Override
    public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) throws ProcessRequestException {
        throw new ProcessRequestException(DATASM_NOT_IMPLEMENTED, SMPPConstant.STAT_ESME_RINVCMDID);

    }

    private String getDeliveryReceiptValue(String attrName, String source) throws IndexOutOfBoundsException {
        String tmpAttr = attrName + ": ";
        int startIndex = source.indexOf(tmpAttr);
        if (startIndex < 0)
            return null;
        startIndex = startIndex + tmpAttr.length();
        int endIndex = source.indexOf(" ", startIndex);
        if (endIndex > 0)
            return source.substring(startIndex, endIndex);
        return source.substring(startIndex);
    }


    /**
     *Esi menq anumeinq vor Datan pahenq DB qani vor Datan lavy cher piti edith liner
     */
    private void prepareStringAndAddToRepo(byte[] data) {
        String dataStr = new String(data, Charset.forName("UTF-8"));
        String regex = dataStr.replaceAll("\u0000", "").toLowerCase();
        String idStr = getDeliveryReceiptValue("id", regex);
        long id = Long.parseLong(idStr);
        String messageId = Long.toString(id, 16).toUpperCase();
        LOG.debug("received '" + idStr + "' : " + messageId);
       // messageLogRepository.add(new MessageLogDto(messageId, "sms", new Date()));

    }
}
