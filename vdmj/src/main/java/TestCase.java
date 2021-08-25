/*******************************************************************************
 *
 *	Copyright (c) 2016 Aarhus University.
 *
 *	Author: Nick Battle and Kenneth Lausdahl
 *
 *	This file is part of VDMJ.
 *
 *	VDMJ is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	VDMJ is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fujitsu.vdmj.messages.ConsolePrintWriter;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ExitException;
import com.fujitsu.vdmj.runtime.StateContext;
import com.fujitsu.vdmj.runtime.VDMOperation;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.VoidValue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.*;


public class TestCase {
    private final static String xmlReportTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n\t";
    private final static String xmlReportSuiteTemplate = "<testsuite xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd\" name=\"%s\" time=\"0.0\" tests=\"0\" errors=\"0\" skipped=\"0\" failures=\"0\"/>";
    private final static String vdmUnitReportEnable = "vdm.unit.report";

    @VDMOperation
    public static Value reflectionRunTest(Value obj, Value name)
            throws Exception {
        String methodName = name.toString().replaceAll("\"", "").trim();

        ObjectValue instance = (ObjectValue) obj;
        for (NameValuePair p : instance.members.asList()) {
            if (p.name.getName().equals(methodName)) {
                if (p.value instanceof OperationValue) {
                    OperationValue opVal = (OperationValue) p.value;
                    Context mainContext = new StateContext(p.name.getLocation(), "reflection scope");

                    mainContext.putAll(ClassInterpreter.getInstance().getInitialContext());
                    // mainContext.putAll(ClassInterpreter.getInstance().);
                    mainContext.setThreadState(ClassInterpreter.getInstance().getInitialContext().threadState.CPU);

                    long timerStart = System.nanoTime();
                    boolean success = false;
                    ExitException error = null;
                    try {
                        opVal.eval(p.name.getLocation(), new ValueList(), mainContext);
                        success = true;
                    } catch (Exception e) {
                        if (e instanceof ExitException) {
                            if(((ExitException)e).value.objectValue(null).type.name.getName().equals("AssertionFailedError"))
                            {
                                success = false;
                            }
                            throw e;
                        }

                        try {
                            return  ClassInterpreter.getInstance().evaluate("Error`throw(\""
                                    + e.getMessage().replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"").replaceAll("\'", "\\\'")
                                    + "\")", mainContext);
                        }catch(ExitException e2)
                        {
                            error = e2;
                            throw e2;
                        }
                    } finally {
                        long totalExecTime = System.nanoTime() - timerStart;

                        if (System.getProperty(vdmUnitReportEnable) != null) {

                            String containerName = "";

                            if (obj instanceof ObjectValue) {
                                containerName = ((ObjectValue) obj).type.name.getName();
                            }

                            recordTestResults(containerName, methodName, success, error, totalExecTime);
                        }
                    }
                }
            }
        }
        return new VoidValue();

    }

    private static void recordTestResults(String containerName, String methodName, boolean success, ExitException error, long totalExecTime) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, TransformerException {

        File report = new File("TEST-" + containerName + ".xml");

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = null;

        if (report.exists()) {
            doc = docBuilder.parse(report);

        } else {
            doc = docBuilder.parse(new ByteArrayInputStream(String.format(xmlReportTemplate + xmlReportSuiteTemplate, containerName).getBytes()));
        }

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile("/testsuite[@name='" + containerName + "']");

        NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

        if (nl.getLength() > 0) {
            Element testSuiteNode = (Element) nl.item(0);

            expr = xpath.compile("testcase[@name='" + methodName + "']");
            NodeList nlT = (NodeList) expr.evaluate(testSuiteNode, XPathConstants.NODESET);
            Element n = null;

            if (nlT.getLength() > 0) {
                n = (Element) nlT.item(0);
            } else {
                n = doc.createElement("testcase");
            }

            while(n.getFirstChild()!=null)
            {
                n.removeChild(n.getFirstChild());
            }

            n.setAttribute("name", methodName);
            n.setAttribute("classname", containerName);
            n.setAttribute("time", totalExecTime * 1E-9 + "");
            testSuiteNode.appendChild(n);

            testSuiteNode.setAttribute("tests", String.valueOf(Integer.parseInt(testSuiteNode.getAttribute("tests")) + 1));

            if (error!=null) {
                testSuiteNode.setAttribute("error", String.valueOf(Integer.parseInt(testSuiteNode.getAttribute("errors")) + 1));
                Element errorElement = doc.createElement("error");
                errorElement.setAttribute("message",error.number+"");
                errorElement.setAttribute("type","ERROR");
                PrintWriter strOut = new PrintWriter(new StringWriter());
                error.ctxt.printStackTrace(new ConsolePrintWriter(strOut),true);
                errorElement.setTextContent(strOut.toString());
                n.appendChild(errorElement);
            } else if (!success) {
                testSuiteNode.setAttribute("failures", String.valueOf(Integer.parseInt(testSuiteNode.getAttribute("failures")) + 1));
                Element failureElement = doc.createElement("failure");
                failureElement.setAttribute("message",methodName);
                failureElement.setAttribute("type","WARNING");
                failureElement.setAttribute("time", totalExecTime * 1E-9 + "");
                n.appendChild(failureElement);
            }

            testSuiteNode.setAttribute("time", String.valueOf(Double.parseDouble(testSuiteNode.getAttribute("time")) + (totalExecTime * 1E-9)));

        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        Result output = new StreamResult(report);
        Source input = new DOMSource(doc);

        transformer.transform(input, output);


    }
}
