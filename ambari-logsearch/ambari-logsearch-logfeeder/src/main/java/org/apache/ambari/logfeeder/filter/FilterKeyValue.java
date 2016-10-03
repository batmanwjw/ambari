/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logfeeder.filter;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.ambari.logfeeder.common.LogfeederException;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.metrics.MetricData;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class FilterKeyValue extends Filter {
  private static final Logger LOG = Logger.getLogger(FilterKeyValue.class);

  private String sourceField = null;
  private String valueSplit = "=";
  private String fieldSplit = "\t";
  private String valueBorders = null;
  
  private MetricData errorMetric = new MetricData("filter.error.keyvalue", false);

  @Override
  public void init() throws Exception {
    super.init();

    sourceField = getStringValue("source_field");
    valueSplit = getStringValue("value_split", valueSplit);
    fieldSplit = getStringValue("field_split", fieldSplit);
    valueBorders = getStringValue("value_borders");

    LOG.info("init() done. source_field=" + sourceField + ", value_split=" + valueSplit + ", " + ", field_split=" +
        fieldSplit + ", " + getShortDescription());
    if (StringUtils.isEmpty(sourceField)) {
      LOG.fatal("source_field is not set for filter. This filter will not be applied");
      return;
    }
  }

  @Override
  public void apply(String inputStr, InputMarker inputMarker) throws LogfeederException {
    apply(LogFeederUtil.toJSONObject(inputStr), inputMarker);
  }

  @Override
  public void apply(Map<String, Object> jsonObj, InputMarker inputMarker) throws LogfeederException {
    if (sourceField == null) {
      return;
    }
    Object valueObj = jsonObj.get(sourceField);
    if (valueObj != null) {
      String splitPattern = Pattern.quote(fieldSplit);
      String[] tokens = valueObj.toString().split(splitPattern);
      for (String nv : tokens) {
        String[] nameValue = getNameValue(nv);
        String name = nameValue != null && nameValue.length == 2 ? nameValue[0] : null;
        String value = nameValue != null && nameValue.length == 2 ? nameValue[1] : null;
        if (name != null && value != null) {
            jsonObj.put(name, value);
         } else {
           logParseError("name=" + name + ", pair=" + nv + ", field=" + sourceField + ", field_value=" + valueObj);
        }
      }
    }
    super.apply(jsonObj, inputMarker);
    statMetric.value++;
  }

  private String[] getNameValue(String nv) {
    if (valueBorders != null) {
      if (nv.charAt(nv.length() - 1) == valueBorders.charAt(1)) {
        String splitPattern = Pattern.quote("" + valueBorders.charAt(0));
        return nv.substring(0, nv.length() - 1).split(splitPattern);
      } else {
        return null;
      }
    }
    else {
      String splitPattern = Pattern.quote(valueSplit);
      return nv.split(splitPattern);
    }
  }

  private void logParseError(String inputStr) {
    errorMetric.value++;
    String logMessageKey = this.getClass().getSimpleName() + "_PARSEERROR";
    LogFeederUtil.logErrorMessageByInterval(logMessageKey, "Error parsing string. length=" + inputStr.length() + ", input=" +
        input.getShortDescription() + ". First upto 100 characters=" + StringUtils.abbreviate(inputStr, 100), null, LOG,
        Level.ERROR);
  }

  @Override
  public String getShortDescription() {
    return "filter:filter=keyvalue,regex=" + sourceField;
  }

  @Override
  public void addMetricsContainers(List<MetricData> metricsList) {
    super.addMetricsContainers(metricsList);
    metricsList.add(errorMetric);
  }
}
