package com.github.wix_maven;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractPackageable extends AbstractWixMojo {

	/**
	 * Output directory <br>
	 * - in future may have 'configuration' appended (default to Release, but not appended yet)<br>
	 * - will have ${arch} appended<br>
	 * - may also have ${culture} appended
	 * 
	 * @parameter expression="${wix.outputDirectory}" default-value="${project.build.directory}/Release"
	 */
	protected File outputDirectory;

	/**
	 * How to build the msi(s) 
	 * <li> base - base culture msi only
	 * <li> default - each culture as full msi
	 * <li> trans - default + transforms & cabs for each additional culture 
	 * <li> repack - trans + the additional cutlure (transform+cab) packed into 1 msi 
	 * note: first culture listed is base
	 * 
	 * @parameter expression="${wix.merge}" default-value="default"
	 */
	protected String mergeLevel;
	public final String ML_REPACK="repack";
	public final String ML_TRANSFORM="trans";
	public final String ML_DEFAULT="default";
	public final String ML_BASE="base";

	/**
	 * What of the steps should be packaged 
	 * <li> default - step output (msi/msp/mst/repack)
	 * <li> wixpdb - the wixpdbs for all cultures
	 * <li> mst - the mst (ie. add to a mergeLevel repack build if used with default)
	 * <li> package - all the msi/msp (ie. add to a mergeLevel trans or repack build if used with default)
	 * <li> culture_cab - externalCabs for each arch/culture
	 * <li> cached_cab - externalCabs from the cabCacheDirectory(s) for each arch
	 * <li> trans_cab - cabs for each arch/culture generated by transform (msi with diff per culture) 
	 * Comma separated value, default is "default,wixpdb,cached_cab"
	 * 
	 * @parameter expression="${wix.pack}" default-value="default,wixpdb,cached_cab"
	 */
	protected String packLevel;
	public final String PL_DEFAULT="default";
	public final String PL_WIXPDB="wixpdb";
	public final String PL_MST="mst";
	public final String PL_PACKAGE="package";
	public final String PL_CACHED_CAB="cached_cab";
	public final String PL_CULTURE_CAB="culture_cab";
	public final String PL_TRANS_CAB="trans_cab";

	/**
	 * The cab cache directory (-cc) only added to light if reuseCab is enabled<br>
	 * - will have ${arch} appended<br>
	 * TODO: make this a formatted item so user can choose to add arch or not
	 * 
	 * @parameter expression="${wix.cabCacheDirectory}" default-value="${project.build.directory}/Release/cabs"
	 */
	protected File cabCacheDirectory;

	/**
	 * The names of cabs that are not embedded
	 * Comma separated value. <br>
	 * ie. in wxs<br>
	 *  &lt;Media Id="1" Cabinet="Product1.cab" EmbedCab="no"/&gt; <br>
	 *  &lt;Media Id="2" Cabinet="Product2.cab" EmbedCab="yes"/&gt; <br>
	 *  &lt;Media Id="3" Cabinet="Product3.bar"/&gt; <br>
	 * Could add <br>
	 *  &lt;externalCabs&gt;Product1.cab,Product3.bar&lt;/externalCabs&gt;
	 *
	 * TODO: this might need to be more like an include/exclude filter list
	 * @parameter expression="${wix.externalCabs}"
	 */
	protected String externalCabs = "";

	/**
	 * A locale is a language id + a culture specification each culture specification can contain a semicolon (;) separated list of cultures, this is an
	 * ordered list to fall back. ie. &gt;cultures&lt;&gt;1033&lt;en-US&gt;/1033&lt;&gt;1031&lt;de-DE;en-US&gt;/1031&lt;&gt;/cultures&lt;
	 * 
	 * Will add to light -cultures:culturespec<br>
	 * Will add to link each culture to the path as part of -b options - maybe should also add langid to path as -b option <br>
	 * Will use language id for re-packing mst
	 * 
	 * @parameter
	 */
	protected Map<String, String> localeList = new LinkedHashMap<String, String>();
// bug: maven gives us a map of it's choice, rather than setting an item at a time, thus losing the prefered ordered set.
	
	/**
	 * Similar to localeList, allow setting from properties as a single string value.<br>
	 * a csv of locale, where a locale is a langId:culturespec, and a culturespec is a semicolon seperate list of cultures. 
	 * ie. 1033:en-US,1031:de-DE;en-US
	 * 
	 * @parameter property="locales" expression="${wix.locales}" default-value="neutral"
	 */
	private String _locales = null;
	// Note: Plexus has bug where a property doesn't allow using an empty/null string - to provide an expression of wix.locales, we have to provide a non null/empty default-value in case wix.locales is null/empty. 

	/**
	 * Target directory for Nar file unpacking. 
	 * 
	 * @parameter expression="${nar.unpackDirectory}" default-value="${project.build.directory}/nar"
	 * @readonly
	 */
	protected File narUnpackDirectory;
	
	public String getLocales() {
		return _locales;
	}

	public void setLocales(String locales) {
		if (locales != null && !locales.isEmpty() && !"neutral".equalsIgnoreCase(locales) ) {
			_locales = locales;
//			getLog().debug("Setting locales from string " + locales);
			for (String locale : _locales.split(",")) {
				String[] splitLocale = locale.split(":", 2);
				if (splitLocale.length == 2) {
					String langId = splitLocale[0].trim();
					String cultureSpec = splitLocale[1].trim();
					if (langId.isEmpty()) {
						getLog().warn("Locale not in correct format - required language Id " + locale);
					} else if (cultureSpec.isEmpty()) {
						getLog().warn("Locale not in correct format - required culturespec " + locale);
					} else {
						localeList.put(langId, cultureSpec);
					}
				} else
					getLog().warn("Locale not in correct format" + locale);
			}
		}
	}

	public AbstractPackageable() {
		super();
	}

	protected void defaultLocale(){
		// TODO: add auto ident cultures?
		if( localeList.isEmpty()){
			getLog().debug("No locales specified");
			localeList.put(null, null);
		}
	}
	
	public final String getPrimaryCulture(String culturespec) {
		if (null != culturespec)
			return culturespec.split(";")[0];
		return culturespec;
	}

	protected File getOutput(String arch, String culturespec, String extension) {
		return getOutput(outputDirectory, arch, getPrimaryCulture(culturespec), extension);
	}

	protected String baseCulturespec(){
		for (String culture : localeList.values() ) {
			return culture;
		}
		return null;
	}
	
	protected Set<String> alternateCulturespecs() {
		Set<String> subcultures = new HashSet<String>();
		if( subcultures != null ){
			Iterator<String> iterator = localeList.values().iterator();
			if( iterator.hasNext()) // discard base
				iterator.next();
			for (; iterator.hasNext();) {
				String culture = iterator.next();
				subcultures.add(culture);
			}
		}
		return subcultures;
	}

	protected Set<String> culturespecs( ) {
		// the culture spec needs to be unique starting from the first primary culture - can turn the collection into a set without loss.
		return new LinkedHashSet<String>( localeList.values() );
	}

	protected Set<String> languages( ) {
		// xml cannot start with number, the keys are ment to be langid numbers, remove a leading _ that makes the key xml friendly.
		HashSet<String> langs = new LinkedHashSet<String>();
		for (String langID : localeList.keySet()) {
			langs.add( langID.replace("_", "") );
		}
		return langs;
	}
	
	protected String[] getExternalCabs( ){
		return externalCabs.split(",+");
	}

	protected String getPackageOutputExtension() {
		if( PACK_BUNDLE.equalsIgnoreCase( packaging ) )
			return "exe";
		else
			return packaging;
	}
	

	protected String getPackaging() {
		return packaging;
	}

	protected void setPackaging(String packaging) {
		this.packaging = packaging;
	}
}