package com.github.wix_maven;

/*
 * #%L WiX Toolset (Windows Installer XML) Maven Plugin %% Copyright (C) 2013 - 2014 GregDomjan
 * NetIQ %% Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License. #L%
 */

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractPackageable extends AbstractWixMojo {

  /**
   * Output directory <br>
   * - in future may have 'configuration' appended (default to Release, but not appended yet)<br>
   * - will have ${arch} appended<br>
   * - may also have ${culture} appended
   */
  @Parameter(property = "wix.outputDirectory", defaultValue = "${project.build.directory}/Release")
  protected File outputDirectory;

  /**
   * How to build the msi(s) <li>base - base culture msi only <li>default - each culture as full msi
   * <li>trans - default + transforms & cabs for each additional culture <li>repack - trans + the
   * additional cutlure (transform+cab) packed into 1 msi note: first culture listed is base
   */
  @Parameter(property = "wix.merge", defaultValue = "default")
  protected String mergeLevel;
  public final String ML_REPACK = "repack";
  public final String ML_TRANSFORM = "trans";
  public final String ML_DEFAULT = "default";
  public final String ML_BASE = "base";

  /**
   * What of the steps should be packaged <li>default - step output (msi/msp/mst/repack) <li>wixpdb
   * - the wixpdbs for all cultures <li>mst - the mst (ie. add to a mergeLevel repack build if used
   * with default) <li>package - all the msi/msp (ie. add to a mergeLevel trans or repack build if
   * used with default) <li>culture_cab - externalCabs for each arch/culture <li>cached_cab -
   * externalCabs from the cabCacheDirectory(s) for each arch <li>trans_cab - cabs for each
   * arch/culture generated by transform (msi with diff per culture) Comma separated value, default
   * is "default,wixpdb,cached_cab"
   */
  @Parameter(property = "wix.pack", defaultValue = "default,wixpdb,cached_cab")
  protected String packLevel;
  public final String PL_DEFAULT = "default";
  public final String PL_WIXPDB = "wixpdb";
  public final String PL_MST = "mst";
  public final String PL_PACKAGE = "package";
  public final String PL_CACHED_CAB = "cached_cab";
  public final String PL_CULTURE_CAB = "culture_cab";
  public final String PL_TRANS_CAB = "trans_cab";

  /**
   * The cab cache directory (-cc) only added to light if reuseCab is enabled<br>
   * - will have ${arch} appended<br>
   * TODO: make this a formatted item so user can choose to add arch or not
   */
  @Parameter(property = "wix.cabCacheDirectory",
      defaultValue = "${project.build.directory}/Release/cabs")
  protected File cabCacheDirectory;

  /**
   * The names of cabs that are not embedded Comma separated value. <br>
   * ie. in wxs<br>
   * &lt;Media Id="1" Cabinet="Product1.cab" EmbedCab="no"/&gt; <br>
   * &lt;Media Id="2" Cabinet="Product2.cab" EmbedCab="yes"/&gt; <br>
   * &lt;Media Id="3" Cabinet="Product3.bar"/&gt; <br>
   * Could add <br>
   * &lt;externalCabs&gt;Product1.cab,Product3.bar&lt;/externalCabs&gt;
   * 
   * TODO: this might need to be more like an include/exclude filter list
   */
  @Parameter(property = "wix.externalCabs", defaultValue = "")
  protected String externalCabs = "";

  /**
   * Target directory for Nar file unpacking.
   */
  @Parameter(property = "wix.narUnpackDirectory", defaultValue = "${project.build.directory}/nar")
  protected File narUnpackDirectory;

  public AbstractPackageable() {
    super();
  }

  protected void defaultLocale() {
    // TODO: add auto ident cultures?
    if (localeList.isEmpty()) {
      getLog().debug("No locales specified");
      localeList.put(null, null);
    }
  }

  protected File getOutput(String arch, String culturespec, String extension) {
    return getOutput(outputDirectory, arch, getPrimaryCulture(culturespec), extension);
  }

  protected String baseCulturespec() {
    for (String culture : localeList.values()) {
      return culture;
    }
    return null;
  }

  protected Set<String> alternateCulturespecs() {
    Set<String> subcultures = new HashSet<String>();
    if (subcultures != null) {
      Iterator<String> iterator = localeList.values().iterator();
      if (iterator.hasNext()) // discard base
        iterator.next();
      for (; iterator.hasNext();) {
        String culture = iterator.next();
        subcultures.add(culture);
      }
    }
    return subcultures;
  }

  protected Set<String> languages() {
    // xml cannot start with number, the keys are ment to be langid numbers, remove a leading _ that
    // makes the key xml friendly.
    HashSet<String> langs = new LinkedHashSet<String>();
    for (String langID : localeList.keySet()) {
      langs.add(langID.replace("_", ""));
    }
    return langs;
  }

  protected String[] getExternalCabs() {
    return externalCabs.split(",+");
  }

  protected String getPackageOutputExtension() {
    if (PACK_BUNDLE.equalsIgnoreCase(packaging))
      return "exe";
    else
      return packaging;
  }


  protected String getPackaging() {
    return packaging;
  }
}
