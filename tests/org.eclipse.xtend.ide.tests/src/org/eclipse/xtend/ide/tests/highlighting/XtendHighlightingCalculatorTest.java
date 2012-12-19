/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.ide.tests.highlighting;

import static org.eclipse.xtext.junit4.ui.util.IResourcesSetupUtil.*;

import java.util.Collection;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtend.core.xtend.XtendClass;
import org.eclipse.xtend.core.xtend.XtendFile;
import org.eclipse.xtend.ide.highlighting.XtendHighlightingCalculator;
import org.eclipse.xtend.ide.highlighting.XtendHighlightingConfiguration;
import org.eclipse.xtend.ide.tests.AbstractXtendUITestCase;
import org.eclipse.xtend.ide.tests.WorkbenchTestHelper;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.syntaxcoloring.DefaultHighlightingConfiguration;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightedPositionAcceptor;
import org.eclipse.xtext.util.StringInputStream;
import org.eclipse.xtext.util.TextRegion;
import org.eclipse.xtext.xbase.ui.highlighting.XbaseHighlightingConfiguration;
import org.junit.Test;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 * @author Holger Schill
 */
public class XtendHighlightingCalculatorTest extends AbstractXtendUITestCase implements IHighlightedPositionAcceptor {

	public static final String DEFAULT_CLASS_DEF = "class Foo";
	
	private  static final String TEST_HELP_CLASS_STRING = "package test; @Deprecated " +
			"public class TestClassDeprecated { " +
			"@Deprecated public static String DEPRECATED_CONSTANT = 'foo'" +
			"@Deprecated public static String CONSTANT = 'foo'" +
			"@Deprecated @Test public void testMethodDeprecated(){} " +
			"@Test public void testMethodNotDeprecated(){} " +
			"@Deprecated public static void testMethodStaticDeprecated(){} " +
			"public static void testMethodStaticNotDeprecated(){}}";
	
	private String classDefString = DEFAULT_CLASS_DEF;

	@Inject
	private XtendHighlightingCalculator calculator;
	
	@Inject
	private WorkbenchTestHelper testHelper;
	
	private Multimap<TextRegion, String> expectedRegions;
	
	private Multimap<TextRegion, String> explicitNotExpectedRegions;
	
	private Set<String> imports;
	
	private Set<String> injects;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		expectedRegions = HashMultimap.create();
		explicitNotExpectedRegions = HashMultimap.create();
		imports = Sets.newHashSet();
		injects = Sets.newHashSet();
		classDefString = DEFAULT_CLASS_DEF;
	}
	
	@Override
	public void tearDown() throws Exception {
		expectedRegions = null;
		explicitNotExpectedRegions = null;
		imports = null;
		injects = null;
		testHelper = null;
		calculator = null;
		super.tearDown();
	}
	
	protected String getPrefix() {
		String prefix = "";
		for(String importDef : imports)
			prefix += importDef + " ";
		prefix += classDefString + " { "; 
		for(String injectDef : injects)
			prefix += injectDef + " ";	
		prefix += "def foo() ";
		return prefix;
	}
	
	protected int getPrefixLength() {
		return getPrefix().length();
	}
	
	protected XtendClass member(String string) throws Exception {
		return clazz(getPrefix()+string+"}");
	}
	
	protected XtendClass clazz(String string) throws Exception {
		return (XtendClass) file(string).getXtendTypes().get(0);
	}

	
	protected XtendFile file(String string) throws Exception {
		createFile(testHelper.getProject().getName() + "/src/test/TestClassDeprecated.java",TEST_HELP_CLASS_STRING);
		waitForAutoBuild();
		ResourceSet set = testHelper.getResourceSet();
		Resource resource = set.createResource(URI.createURI("Foo.xtend"));
		resource.load(new StringInputStream(string), null);
		
		assertEquals(resource.getErrors().toString(), 0, resource.getErrors().size());
		XtendFile file = (XtendFile) resource.getContents().get(0);
		return file;
	}
	
	@Test public void testEmptyString() {
		expectInsignificant(0, 3);
		expectInsignificant(3, 3);
		highlight("''''''");
	}
	
	@Test public void testStringWithWS() {
		expectInsignificant(0, 3);
		expectInsignificant(5, 3);
		highlight("'''  '''");
	}
	
	@Test public void testSingleLineLiteral() {
		expectInsignificant(0, 3);
		expectInsignificant(9, 3);
		highlight(
				"'''foobar'''");
	}
	
	@Test public void testLiteral() {
		expectInsignificant(0, 3);
		expectInsignificant(3, 2);
		expectInsignificant(6, 1);
		expectInsignificant(15,3);
		expectAbsolute(14,1,XtendHighlightingConfiguration.TEMPLATE_LINE_BREAK);
		highlight(
				"'''  \n" +
				" foobar \n" +
				"'''");
	}
	
	@Test public void testLiteralsWithComments() {
		expectInsignificant(0, 3);
		expectInsignificant(3, 2);
		expectInsignificant(6, 2);
		expectAbsolute(18, 1, XtendHighlightingConfiguration.TEMPLATE_LINE_BREAK);
		expectInsignificant(19, 1);
		expectInsignificant(37, 1);
		expectAbsolute(50, 1, XtendHighlightingConfiguration.TEMPLATE_LINE_BREAK);
		expectInsignificant(51, 2);
		expectAbsolute(86, 1, XtendHighlightingConfiguration.TEMPLATE_LINE_BREAK);
		expectInsignificant(87, 3);
		highlight(
				"'''  \n" +
				"  first line\n" +
				" � /* comment */ � second line \n" +
				"  third�\n" +
				"   /* comment */ \n" +
				" � line \n" +
				"'''");
	}
	
	@Test public void testBug375272_01() {
		expectInsignificant(0, 3);
		expectInsignificant(4, 6);
		expectAbsolute(20, 11, DefaultHighlightingConfiguration.COMMENT_ID);
		expectInsignificant(32, 6);
		expectInsignificant(46, 3);
		highlight(
				"'''\n" + 
				"      �IF true�\n" + 
				"��� comment\n" + 
				"      �ENDIF�\n" + 
				"'''");
	}
	
	@Test public void testBug375272_02() {
		expectInsignificant(0, 3);
		expectInsignificant(5, 6);
		expectAbsolute(22, 11, DefaultHighlightingConfiguration.COMMENT_ID);
		expectInsignificant(35, 6);
		expectInsignificant(50, 3);
		highlight(
				"'''\r\n" + 
				"      �IF true�\r\n" + 
				"��� comment\r\n" + 
				"      �ENDIF�\r\n" + 
				"'''");
	}
	
	@Test public void testExpression() {
		String model = 
			"'''\n" +
			"\t\t�'foobar'� \n" +
			"  '''";
		expectInsignificant(0, 3);
		expectInsignificant(model.indexOf('\t'), 2);
		expectInsignificant(model.indexOf("  "), 2);
		expectInsignificant(model.lastIndexOf("'''"), 3);
		expectAbsolute(17,1,XtendHighlightingConfiguration.POTENTIAL_LINE_BREAK);
		highlight(model);
	}
	
	@Test public void testSingleLineExpression() {
		String model = "'''�'literal'�'''";
		expectInsignificant(0, 3);
		expectInsignificant(14, 3);
		highlight(model);
	}
	
	@Test public void testVoid() {
		String model = "{ var void v = null }";
		expectAbsolute(model.indexOf("void"), 4, DefaultHighlightingConfiguration.KEYWORD_ID);
		highlight(model);
	}
	
	@Test public void testThis() {
		String model = "{ var f = this }";
		expectAbsolute(model.indexOf("this"), 4, DefaultHighlightingConfiguration.KEYWORD_ID);
		highlight(model);
	}
	
	@Test public void testInt() {
		String model = "{ var int i = 1 }";
		expectAbsolute(model.indexOf("int"), 3, DefaultHighlightingConfiguration.KEYWORD_ID);
		expectAbsolute(model.lastIndexOf("1"), 1, DefaultHighlightingConfiguration.NUMBER_ID);
		highlight(model);
	}
	
	@Test public void testCreateAsIdentifier() {
		String model = "{ var String create = '' }";
		expectAbsolute(model.indexOf("create"), 6, DefaultHighlightingConfiguration.DEFAULT_ID);
		highlight(model);
	}
	
	@Test public void testCreateKeyword() {
		String model = "{} def create result: new Object() create() {}";
		expectAbsolute(model.lastIndexOf("create"), 6, DefaultHighlightingConfiguration.DEFAULT_ID);
		highlight(model);
	}
	
	@Test public void testStaticFieldAccess() throws Exception {
		String model = "{ Integer::MAX_VALUE }";
		expectAbsolute(model.lastIndexOf("MAX_VALUE"), 9, XbaseHighlightingConfiguration.STATIC_FIELD);
		highlight(model);
	}
	
	@Test public void testStaticOperationInvocation() throws Exception {
		addImport("java.util.Collections");
		String model = "{ Collections::emptySet  }";
		expectAbsolute(model.lastIndexOf("emptySet"), 8, XbaseHighlightingConfiguration.STATIC_METHOD_INVOCATION);
		notExpectAbsolute(model.lastIndexOf("emptySet"), 8, XbaseHighlightingConfiguration.EXTENSION_METHOD_INVOCATION);
		highlight(model);
	}
	@Test public void testStaticExtensionOperationInvocation() throws Exception {
		String model = "{ 'FOO'.toFirstLower   }";
		expectAbsolute(model.lastIndexOf("toFirstLower"), 12, XbaseHighlightingConfiguration.STATIC_METHOD_INVOCATION);
		expectAbsolute(model.lastIndexOf("toFirstLower"), 12, XbaseHighlightingConfiguration.EXTENSION_METHOD_INVOCATION);
		highlight(model);
	}
	
	@Test public void testNonStaticExtensionOperationInvocation() throws Exception {
		addImport("com.google.inject.Inject");
		addInject("extension StringBuilder");
		String model = "{ 'Foo'.append   }";
		expectAbsolute(model.lastIndexOf("append"), 6, XbaseHighlightingConfiguration.EXTENSION_METHOD_INVOCATION);
		notExpectAbsolute(model.lastIndexOf("append"), 6, XbaseHighlightingConfiguration.STATIC_METHOD_INVOCATION);
		highlight(model);
	}
	
	@Test public void testStaticImportedExtensionOperationInvocation() throws Exception {
		addImport("static extension java.util.Collections.*");
		String model = "{ newArrayList.shuffle   }";
		expectAbsolute(model.lastIndexOf("newArrayList"), 12, XbaseHighlightingConfiguration.STATIC_METHOD_INVOCATION);
		notExpectAbsolute(model.lastIndexOf("newArrayList"), 12, XbaseHighlightingConfiguration.EXTENSION_METHOD_INVOCATION);
		expectAbsolute(model.lastIndexOf("shuffle"), 7, XbaseHighlightingConfiguration.EXTENSION_METHOD_INVOCATION);
		expectAbsolute(model.lastIndexOf("shuffle"), 7, XbaseHighlightingConfiguration.STATIC_METHOD_INVOCATION);
		highlight(model);
	}
	
	@Test public void testStaticExtensionOperationWithNoImplicitArguments() throws Exception {
		addImport("java.util.List");
		String model = "def toUpperCase(List<String> it) { map [ toUpperCase ]}";
		expectAbsolute(model.lastIndexOf("map"), 3, XbaseHighlightingConfiguration.EXTENSION_METHOD_INVOCATION);
		expectAbsolute(model.lastIndexOf("map"), 3, XbaseHighlightingConfiguration.STATIC_METHOD_INVOCATION);
		highlight(model);
	}
	
	@Test public void testLocalExtensionOperation() throws Exception {
		addImport("java.util.List");
		String model = "def void zonk(List<String> it) { zonk }";
		expectAbsolute(model.lastIndexOf("zonk"), 4, XbaseHighlightingConfiguration.EXTENSION_METHOD_INVOCATION);
		highlight(model);
	}

	@Test public void testBug377413(){
		addImport("java.util.List");
		String model = "def void bar(Foo it){ zonk = '42' } def setZonk(String x){} def void fooBar(List<String> it) { fooBar }";
		notExpectAbsolute(model.indexOf("zonk"),4,XbaseHighlightingConfiguration.EXTENSION_METHOD_INVOCATION);
		expectAbsolute(model.lastIndexOf("fooBar"), 6, XbaseHighlightingConfiguration.EXTENSION_METHOD_INVOCATION);
		highlight(model);
	}
	
	@Test public void testAnnotaton() throws Exception {
		addImport("com.google.inject.Inject");
		String model = "{} @Inject extension StringBuilder";
		expectAbsolute(model.lastIndexOf("@"), 1,XbaseHighlightingConfiguration.ANNOTATION);
		expectAbsolute(model.lastIndexOf("Inject"), 6, XbaseHighlightingConfiguration.ANNOTATION);
		highlight(model);
	}
	
	@Test public void testAnnotatonWithValues() throws Exception {
		addImport("com.google.inject.name.Named");
		String model = "@Named(value='bar') def foo()";
		expectAbsolute(model.lastIndexOf("@"), 1,XbaseHighlightingConfiguration.ANNOTATION);
		expectAbsolute(model.lastIndexOf("Named"), 5, XbaseHighlightingConfiguration.ANNOTATION);
		notExpectAbsolute(model.indexOf("(value=42)"),10 , XbaseHighlightingConfiguration.ANNOTATION);
		highlight(model);
	}
	
	@Test public void testReferencedJvmAnnotatonType() throws Exception {
		addImport("com.google.inject.Inject");
		String model = "{ val bar = typeof(Inject) } ";
		expectAbsolute(model.lastIndexOf("Inject"), 6, XbaseHighlightingConfiguration.ANNOTATION);
		highlight(model);
	}
	
	@Test public void testXtendFieldDeclaration() throws Exception {
		addImport("com.google.inject.Inject");
		String model = "{} @Inject StringBuilder bar";
		expectAbsolute(model.lastIndexOf("@"), 1,XbaseHighlightingConfiguration.ANNOTATION);
		expectAbsolute(model.lastIndexOf("Inject"), 6, XbaseHighlightingConfiguration.ANNOTATION);
		notExpectAbsolute(model.lastIndexOf("bar"), 3, XbaseHighlightingConfiguration.STATIC_FIELD);
		highlight(model);
	}
	
	@Test public void testNonStaticFieldAccess() throws Exception {
		addImport("com.google.inject.Inject");
		String model = "{} @Inject StringBuilder bar def testFunction() { bar.append('foo') }";
		expectAbsolute(model.lastIndexOf("@"), 1,XbaseHighlightingConfiguration.ANNOTATION);
		expectAbsolute(model.lastIndexOf("Inject"), 6, XbaseHighlightingConfiguration.ANNOTATION);
		expectAbsolute(model.indexOf("bar"), 3, XbaseHighlightingConfiguration.FIELD);
		notExpectAbsolute(model.indexOf("bar"), 3, XbaseHighlightingConfiguration.STATIC_FIELD);
		expectAbsolute(model.lastIndexOf("bar"), 3, XbaseHighlightingConfiguration.FIELD);
		notExpectAbsolute(model.lastIndexOf("bar"), 3, XbaseHighlightingConfiguration.STATIC_FIELD);
		highlight(model);
	}
	
	
	@Test public void testDeprecatedXtendClass() throws Exception {
		classDefString = "@Deprecated class Bar";
		String model = "{}";
		expect(getPrefix().lastIndexOf("@"), 1,XbaseHighlightingConfiguration.ANNOTATION);
		expect(getPrefix().lastIndexOf("Deprecated"), 10, XbaseHighlightingConfiguration.ANNOTATION);
		expect(getPrefix().indexOf("Bar"), 3, XbaseHighlightingConfiguration.DEPRECATED_MEMBERS);
		highlight(model);
	}
	
	@Test public void testDeprecatedParentClass() throws Exception {
		addImport("test.TestClassDeprecated");
		classDefString = "class Bar extends TestClassDeprecated";
		String model = "{}";
		expect(getPrefix().lastIndexOf("TestClassDeprecated"), 19, XbaseHighlightingConfiguration.DEPRECATED_MEMBERS);
		highlight(model);
	}

	@Test public void testDeprecatedStaticFieldAccess() throws Exception {
		addImport("test.TestClassDeprecated");
		String model = "{TestClassDeprecated::DEPRECATED_CONSTANT}";
		expectAbsolute(model.lastIndexOf("DEPRECATED_CONSTANT"), 19, XbaseHighlightingConfiguration.STATIC_FIELD);
		expectAbsolute(model.lastIndexOf("DEPRECATED_CONSTANT"), 19, XbaseHighlightingConfiguration.DEPRECATED_MEMBERS);
		highlight(model);
	}
	
	@Test public void testDeprecatedMethodAccess() throws Exception {
		addImport("test.TestClassDeprecated");
		addImport("com.google.inject.Inject");
		String model = "{} @Inject TestClassDeprecated clazz def baz(){ clazz.testMethodDeprecated() }";
		expectAbsolute(model.lastIndexOf("@"), 1,XbaseHighlightingConfiguration.ANNOTATION);
		expectAbsolute(model.indexOf("Inject"), 6,XbaseHighlightingConfiguration.ANNOTATION);
		expectAbsolute(model.indexOf("clazz"), 5, XbaseHighlightingConfiguration.FIELD);
		expectAbsolute(model.lastIndexOf("clazz"), 5, XbaseHighlightingConfiguration.FIELD);
		expectAbsolute(model.lastIndexOf("testMethodDeprecated"), 20, XbaseHighlightingConfiguration.DEPRECATED_MEMBERS);
		highlight(model);
	}
	
	@Test public void testNotDeprecatedMethodAccess() throws Exception {
		addImport("test.TestClassDeprecated");
		addImport("com.google.inject.Inject");
		String model = "{} @Inject TestClassDeprecated clazz def baz(){ clazz.testMethodNotDeprecated() }";
		expectAbsolute(model.lastIndexOf("@"), 1,XbaseHighlightingConfiguration.ANNOTATION);
		expectAbsolute(model.indexOf("Inject"), 6,XbaseHighlightingConfiguration.ANNOTATION);
		expectAbsolute(model.indexOf("clazz"), 5, XbaseHighlightingConfiguration.FIELD);
		expectAbsolute(model.lastIndexOf("clazz"), 5, XbaseHighlightingConfiguration.FIELD);
		notExpectAbsolute(model.indexOf("testMethodNotDeprecated"), 23, XbaseHighlightingConfiguration.DEPRECATED_MEMBERS);
		highlight(model);
	}
	
	@Test public void testDeprecatedStaticMethodAccess() throws Exception {
		addImport("test.TestClassDeprecated");
		String model = "{TestClassDeprecated::testMethodStaticDeprecated() }";
		expectAbsolute(model.lastIndexOf("testMethodStaticDeprecated"), 26, XbaseHighlightingConfiguration.STATIC_METHOD_INVOCATION);
		expectAbsolute(model.lastIndexOf("testMethodStaticDeprecated"), 26, XbaseHighlightingConfiguration.DEPRECATED_MEMBERS);
		highlight(model);
	}
	
	@Test public void testNotDeprecatedStaticMethodAccess() throws Exception {
		addImport("test.TestClassDeprecated");
		String model = "{TestClassDeprecated::testMethodStaticNotDeprecated() }";
		expectAbsolute(model.lastIndexOf("testMethodStaticNotDeprecated"), 29, XbaseHighlightingConfiguration.STATIC_METHOD_INVOCATION);
		notExpectAbsolute(model.lastIndexOf("testMethodStaticNotDeprecated"), 29, XbaseHighlightingConfiguration.DEPRECATED_MEMBERS);
		highlight(model);
	}
	
	@Test public void testDeprecatedXtendField() throws Exception {
		addImport("test.TestClassDeprecated");
		addImport("com.google.inject.Inject");
		String model = "{} @Deprecated @Inject TestClassDeprecated clazz def baz(){ clazz.testMethodNotDeprecated() }";
		expectAbsolute(model.indexOf("@"), 1,XbaseHighlightingConfiguration.ANNOTATION);
		expectAbsolute(model.indexOf("Deprecated"), 10,XbaseHighlightingConfiguration.ANNOTATION);
		expectAbsolute(model.lastIndexOf("@"), 1,XbaseHighlightingConfiguration.ANNOTATION);
		expectAbsolute(model.indexOf("Inject"), 6,XbaseHighlightingConfiguration.ANNOTATION);
		expectAbsolute(model.indexOf("clazz"), 5, XbaseHighlightingConfiguration.FIELD);
		expectAbsolute(model.indexOf("clazz"), 5, XbaseHighlightingConfiguration.DEPRECATED_MEMBERS);
		expectAbsolute(model.lastIndexOf("clazz"), 5, XbaseHighlightingConfiguration.FIELD);
		expectAbsolute(model.lastIndexOf("clazz"), 5,XbaseHighlightingConfiguration.DEPRECATED_MEMBERS);
		notExpectAbsolute(model.indexOf("testMethodNotDeprecated"), 23, XbaseHighlightingConfiguration.DEPRECATED_MEMBERS);
		highlight(model);
	}
	
	@Test public void testDeprecatedXtendFunction() throws Exception {
		String model = "{} @Deprecated def baz(){} def bar(){ baz()}";
		expectAbsolute(model.lastIndexOf("@"), 1,XbaseHighlightingConfiguration.ANNOTATION);
		expectAbsolute(model.indexOf("Deprecated"), 10,XbaseHighlightingConfiguration.ANNOTATION);
		expectAbsolute(model.indexOf("baz"), 3, XbaseHighlightingConfiguration.DEPRECATED_MEMBERS);
		expectAbsolute(model.lastIndexOf("baz"), 3, XbaseHighlightingConfiguration.DEPRECATED_MEMBERS);
		highlight(model);
	}
	
	@Test public void testDeclaredStaticField() throws Exception {
		String model = "{} private static String foo def bar() {foo}";
		expectAbsolute(model.indexOf("foo"), 3,XbaseHighlightingConfiguration.STATIC_FIELD);
		expectAbsolute(model.lastIndexOf("foo"), 3,XbaseHighlightingConfiguration.STATIC_FIELD);
		highlight(model);
	}
	
	
	protected void highlight(String functionBody) {
		try {
			XtendClass model = member(functionBody);
			calculator.provideHighlightingFor((XtextResource) model.eResource(), this);
			assertTrue(expectedRegions.toString(), expectedRegions.isEmpty());
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	protected void addImport(String importString){
		imports.add("import " + importString);
	}
	
	protected void addInject(String injectString) {
		injects.add("@Inject " + injectString);
	}

	protected void expectInsignificant(int offset, int length) {
		expectAbsolute(offset, length, XtendHighlightingConfiguration.INSIGNIFICANT_TEMPLATE_TEXT);
	}

	protected void expect(int offset, int length, String highlightID) {
		expectedRegions.put(new TextRegion(offset, length), highlightID);
	}
	
	protected void expectAbsolute(int offset, int length, String highlightID) {
		expect(offset + getPrefixLength(), length, highlightID);
	}
	
	protected void notExpect(int offset, int length, String highlightID){
		explicitNotExpectedRegions.put(new TextRegion(offset, length), highlightID);
	}
	
	protected void notExpectAbsolute(int offset, int length, String highlightID){
		notExpect(offset + getPrefixLength(), length, highlightID);
	}

	public void addPosition(int offset, int length, String... ids) {
//		System.out.print("acceptor.addPosition(" + offset + ", " + length);
//		for(String id: ids) {
//			System.out.print(", \"" + id + "\"");	
//		}
//		System.out.println(");");
		assertTrue("length = " + length, length >= 0);
		TextRegion region = new TextRegion(offset, length);
		assertEquals(1, ids.length);
		assertFalse(region.toString(), expectedRegions.isEmpty());
		Collection<String> expectedIds = expectedRegions.get(region);
		if(expectedIds.size() > 0)
			assertTrue("expected: " + expectedRegions.toString() + " but was: " + region + " (" + ids[0] + ")", expectedIds.contains(ids[0]));
		if(expectedIds.contains(ids[0]))
			expectedRegions.remove(region, ids[0]);
		Collection<String> unexpectedIds = explicitNotExpectedRegions.get(region);
		assertFalse("unexpected highlighting as position: " + region + " (" + ids[0] + ")", unexpectedIds.contains(ids[0]));
	}
}
