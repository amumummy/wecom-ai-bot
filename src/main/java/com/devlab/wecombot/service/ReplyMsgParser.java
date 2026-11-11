package com.devlab.wecombot.service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * 企微明文回复 XML 解析（含 XXE 防护），提取 FromUserName/Content/MsgId/MsgType。
 */
public class ReplyMsgParser {

    public static class ReplyMsg {
        public String fromUser;
        public String content;
        public String msgId;
        public String msgType;
    }

    public static ReplyMsg parse(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);

        Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        Element root = doc.getDocumentElement();

        ReplyMsg m = new ReplyMsg();
        m.fromUser = text(root, "FromUserName");
        m.content = text(root, "Content");
        m.msgId = text(root, "MsgId");
        m.msgType = text(root, "MsgType");
        return m;
    }

    private static String text(Element root, String tag) {
        if (root.getElementsByTagName(tag).getLength() > 0) {
            return root.getElementsByTagName(tag).item(0).getTextContent();
        }
        return "";
    }
}
