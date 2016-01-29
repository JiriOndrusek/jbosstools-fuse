/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 

package org.fusesource.ide.camel.editor.internal;

import org.eclipse.osgi.util.NLS;

/**
 * @author lhein
 */
public class UIMessages extends NLS {

	private static final String BUNDLE_NAME = "org.fusesource.ide.camel.editor.internal.l10n.messages";

    public static String editorSourcePageTitle;
    public static String editorDesignPageTitle;
    public static String editorGlobalConfigurationPageTitle;
    public static String connectorsDrawerTitle;
    public static String endpointsDrawerTitle;
    public static String routingDrawerTitle;
    public static String controlFlowDrawerTitle;
    public static String transformationDrawerTitle;
    public static String miscellaneousDrawerTitle;

    public static String propertiesDetailsTitle;
    public static String propertiesDocumentationTitle;
    
    public static String unconnectedNodeFoundTitle;
    public static String unconnectedNodeFoundText;
    
    public static String failedXMLValidationTitle;
    public static String failedXMLValidationText;
    
    public static String editorPreferencePageDescription;
	public static String editorPreferencePageDefaultLanguageSetting;
	public static String editorPreferencePagePreferIdAsLabelSetting;
	public static String editorPreferencePageLayoutOrientationSetting;
	public static String editorPreferencePageLayoutOrientationSOUTH;
	public static String editorPreferencePageLayoutOrientationEAST;
	public static String editorPreferencePageGridVisibilitySetting;

	public static String colorPreferencePageDescription;
	public static String colorPreferencePageGridColorSetting;
	public static String colorPreferencePageTextColorSetting;
	public static String colorPreferencePageConnectionColorSetting;
	public static String colorPreferencePageFigureBGColorSetting;
	public static String colorPreferencePageFigureFGColorSetting;
	public static String colorPreferencePageTableChartBGColorSetting;
	
	public static String autoLayoutActionDescription;
	public static String autoLayoutActionLabel;
	public static String autoLayoutActionTooltip;
    
	public static String createGlobalElementDialogTitle;
	public static String createGlobalElementDiaglogText;
	
	public static String globalElementsTabAddButtonLabel;
	public static String globalElementsTabAddButtonTooltip;
	public static String globalElementsTabEditButtonLabel;
	public static String globalElementsTabEditButtonTooltip;
	public static String globalElementsTabDeleteButtonLabel;
	public static String globalElementsTabDeleteButtonTooltip;
	
	public static String newGlobalConfigurationTypeWizardDialogTitle;
	public static String CamelDiagramBehaviour_messageOnErrorEditorInitialization;
	
	
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, UIMessages.class);
    }
}
