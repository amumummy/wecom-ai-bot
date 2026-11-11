package com.devlab.wecombot.crypto;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * 从企微回调的 XML 中提取 Encrypt / ToUserName 字段（带 XXE 防护）
 */
public final class XMLParse {

    private XMLParse() {
    }

    /**
     * @return [0]=Encrypt 密文内容（可能为 null），[1]=ToUserName（可能为 null）
     */
    public static String[] extract(String xml) throws AesException {
        String[] result = new String[2];
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // XXE 防护：禁用外部实体
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);

            InputSource is = new InputSource(new StringReader(xml));
            Element root = dbf.newDocumentBuilder().parse(is).getDocumentElement();

            NodeList encryptList = root.getElementsByTagName("Encrypt");
            if (encryptList.getLength() > 0) {
                result[0] = encryptList.item(0).getTextContent();
            }
            NodeList touserList = root.getElementsByTagName("ToUserName");
            if (touserList.getLength() > 0) {
                result[1] = touserList.item(0).getTextContent();
            }
            return result;
        } catch (Exception e) {
            throw new AesException(AesException.ParseXmlError);
        }
    }
}
