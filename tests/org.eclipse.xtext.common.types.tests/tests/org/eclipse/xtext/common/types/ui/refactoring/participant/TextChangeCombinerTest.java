/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.common.types.ui.refactoring.participant;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.xtext.junit4.ui.util.IResourcesSetupUtil;
import org.eclipse.xtext.ui.refactoring.impl.DisplayChangeWrapper;
import org.eclipse.xtext.ui.refactoring.impl.FixedCompositeChange;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Jan Koehnlein - Initial contribution and API
 */
public class TextChangeCombinerTest {

	private static final String PROJECT = "text.change.combiner.test";

	private static final String MODEL = "0123456789";

	private static final String TEXT_TYPE = "testtype";

	private TextChangeCombiner combiner = new TextChangeCombiner();

	private IFile file0;

	@Before
	public void setUp() throws Exception {
		file0 = IResourcesSetupUtil.createFile(PROJECT + "/file0.txt", MODEL);
	}

	@After
	public void closeEditors() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
	}
	
	@AfterClass
	public static void tearDown() throws Exception {
		ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT).delete(true, new NullProgressMonitor());
	}

	@Test
	public void testSingleFileChange() throws Exception {
		Change textFileChange = createTextFileChange(file0, 1, 1, "foo");
		Change combined = combiner.combineChanges(textFileChange);
		assertTextType(combined);
		assertEquals(textFileChange, combined);
		Change undo = combined.perform(new NullProgressMonitor());
		assertEquals(MODEL.replace("1", "foo"), getContents(file0));
		undo.perform(new NullProgressMonitor());
		assertEquals(MODEL, getContents(file0));
	}

	@Test
	public void testSingleDocumentChange() throws Exception {
		IDocument document = openDocument(file0);
		Change docChange = createDocumentChange(document, 1, 1, "foo");
		Change combined = combiner.combineChanges(docChange);
		assertEquals(docChange, combined);
		assertTextType(combined);
		Change undo = combined.perform(new NullProgressMonitor());
		assertEquals(MODEL.replace("1", "foo"), document.get());
		undo.perform(new NullProgressMonitor());
		assertEquals(MODEL, document.get());
	}

	@Test
	public void testMultipleFileChanges() throws Exception {
		FixedCompositeChange compositeChange = new FixedCompositeChange("test");
		compositeChange.add(createTextFileChange(file0, 1, 1, "foo"));
		compositeChange.add(createTextFileChange(file0, 2, 1, "bar"));
		FixedCompositeChange compositeChange1 = new FixedCompositeChange("test");
		compositeChange.add(compositeChange1);
		compositeChange1.add(createTextFileChange(file0, 3, 1, "baz"));
		compositeChange1.add(createTextFileChange(file0, 2, 1, "bar"));
		compositeChange1.add(createMultiTextFileChange(file0, 1, 1, "foo", 4, 1, "foo"));
		Change combined = combiner.combineChanges(compositeChange);
		assertTrue(combined instanceof CompositeChange);
		assertEquals(1, ((CompositeChange) combined).getChildren().length);
		Change combinedChild = ((CompositeChange) combined).getChildren()[0];
		assertTrue(combinedChild instanceof DisplayChangeWrapper.Wrapper);
		Change delegate = ((DisplayChangeWrapper.Wrapper) combinedChild).getDelegate();
		assertTextType(delegate);
		Change undo = combined.perform(new NullProgressMonitor());
		assertEquals(MODEL.replace("1234", "foobarbazfoo"), getContents(file0));
		undo.perform(new NullProgressMonitor());
		assertEquals(MODEL, getContents(file0));
	}

	@Test
	public void testMultipleDocumentChanges() throws Exception {
		IDocument document = openDocument(file0);
		FixedCompositeChange compositeChange = new FixedCompositeChange("test");
		compositeChange.add(createDocumentChange(document, 1, 1, "foo"));
		compositeChange.add(createDocumentChange(document, 2, 1, "bar"));
		FixedCompositeChange compositeChange1 = new FixedCompositeChange("test");
		compositeChange.add(compositeChange1);
		compositeChange1.add(createDocumentChange(document, 3, 1, "baz"));
		compositeChange1.add(createDocumentChange(document, 2, 1, "bar"));
		compositeChange1.add(createMultiDocumentChange(document, 1, 1, "foo", 4, 1, "foo"));
		Change combined = combiner.combineChanges(compositeChange);
		assertTrue(combined instanceof CompositeChange);
		assertEquals(1, ((CompositeChange) combined).getChildren().length);
		Change combinedChild = ((CompositeChange) combined).getChildren()[0];
		assertTrue(combinedChild instanceof DisplayChangeWrapper.Wrapper);
		Change delegate = ((DisplayChangeWrapper.Wrapper) combinedChild).getDelegate();
		assertTextType(delegate);
		Change undo = combined.perform(new NullProgressMonitor());
		assertEquals(MODEL.replace("1234", "foobarbazfoo"), document.get());
		undo.perform(new NullProgressMonitor());
		assertEquals(MODEL, document.get());
	}

	@Test
	public void testMixedChanges() throws Exception {
		IFile file1 = IResourcesSetupUtil.createFile(PROJECT + "/file1.txt", MODEL);
		IDocument document = openDocument(file1);
		FixedCompositeChange compositeChange = new FixedCompositeChange("test");
		compositeChange.add(createDocumentChange(document, 1, 1, "foo"));
		compositeChange.add(createTextFileChange(file0, 1, 1, "foo"));
		FixedCompositeChange compositeChange1 = new FixedCompositeChange("test");
		compositeChange.add(compositeChange1);
		compositeChange1.add(createDocumentChange(document, 3, 1, "baz"));
		compositeChange1.add(createTextFileChange(file0, 1, 1, "foo"));
		compositeChange1.add(createTextFileChange(file0, 3, 1, "baz"));
		Change combined = combiner.combineChanges(compositeChange);
		Change undo = combined.perform(new NullProgressMonitor());
		assertEquals(MODEL.replace("123", "foo2baz"), document.get());
		assertEquals(MODEL.replace("123", "foo2baz"), getContents(file0));
		undo.perform(new NullProgressMonitor());
		assertEquals(MODEL, document.get());
		assertEquals(MODEL, getContents(file0));
	}

	@Test
	public void testEmptyChange() throws Exception {
		FixedCompositeChange emptyChange = new FixedCompositeChange("test");
		assertNull(combiner.combineChanges(emptyChange));
	}

	protected void assertTextType(Change change) {
		if (change instanceof CompositeChange) {
			for (Change child : ((CompositeChange) change).getChildren()) {
				assertTextType(child);
			}
		} else {
			assertTrue(change.getClass().getName(), change instanceof TextEditBasedChange);
			assertEquals(TEXT_TYPE, ((TextEditBasedChange) change).getTextType());
		}
	}

	protected IDocument openDocument(IFile file) throws PartInitException {
		FileEditorInput fileEditorInput = new FileEditorInput(file);
		AbstractTextEditor editor = (AbstractTextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().openEditor(fileEditorInput, "org.eclipse.ui.DefaultTextEditor");
		IDocumentProvider documentProvider = editor.getDocumentProvider();
		return documentProvider.getDocument(fileEditorInput);
	}

	protected Change createTextFileChange(IFile file, int offset, int length, String replacement) {
		ReplaceEdit edit = new ReplaceEdit(offset, length, replacement);
		TextFileChange textFileChange = new TextFileChange("text change", file);
		textFileChange.setEdit(edit);
		textFileChange.setTextType(TEXT_TYPE);
		return textFileChange;
	}

	protected Change createMultiTextFileChange(IFile file, int offset0, int length0, String replacement0, int offset1,
			int length1, String replacement1) {
		MultiTextEdit multiTextEdit = createMultiTextEdit(offset0, length0, replacement0, offset1, length1,
				replacement1);
		TextFileChange textFileChange = new TextFileChange("text change", file);
		textFileChange.setEdit(multiTextEdit);
		textFileChange.setTextType(TEXT_TYPE);
		return textFileChange;
	}

	protected MultiTextEdit createMultiTextEdit(int offset0, int length0, String replacement0, int offset1,
			int length1, String replacement1) {
		ReplaceEdit edit0 = new ReplaceEdit(offset0, length0, replacement0);
		ReplaceEdit edit1 = new ReplaceEdit(offset1, length1, replacement1);
		MultiTextEdit multiTextEdit = new MultiTextEdit();
		multiTextEdit.addChild(edit0);
		multiTextEdit.addChild(edit1);
		return multiTextEdit;
	}

	protected Change createDocumentChange(IDocument document, int offset, int length, String replacement) {
		ReplaceEdit edit = new ReplaceEdit(offset, length, replacement);
		DocumentChange documentChange = new DocumentChange("document change", document);
		documentChange.setEdit(edit);
		documentChange.setTextType(TEXT_TYPE);
		return documentChange;
	}

	protected Change createMultiDocumentChange(IDocument document, int offset0, int length0, String replacement0,
			int offset1, int length1, String replacement1) {
		MultiTextEdit multiTextEdit = createMultiTextEdit(offset0, length0, replacement0, offset1, length1,
				replacement1);
		DocumentChange documentChange = new DocumentChange("text change", document);
		documentChange.setEdit(multiTextEdit);
		documentChange.setTextType(TEXT_TYPE);
		return documentChange;
	}

	protected String getContents(IFile file) throws Exception {
		InputStream inputStream = file.getContents();
		try {
			byte[] buffer = new byte[2048];
			int bytesRead = 0;
			StringBuffer b = new StringBuffer();
			do {
				bytesRead = inputStream.read(buffer);
				if (bytesRead != -1)
					b.append(new String(buffer, 0, bytesRead));
			} while (bytesRead != -1);
			return b.toString();
		} finally {
			inputStream.close();
		}
	}

}
