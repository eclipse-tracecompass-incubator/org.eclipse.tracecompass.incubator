/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser;

import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.AbnormalType.LAST_TYPE;

import java.util.Comparator;
import java.util.List;

import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.TimedEvent;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.I18nStringView;

public class AbnormalPoint {
    private AbnormalType type;
    private TimedEvent site;
    private List<I18nStringView> defaultSuggestions;

    public static final AbnormalPoint LEAST_SERIOUS = new AbnormalPoint(LAST_TYPE, null);

    public AbnormalPoint(AbnormalType type, TimedEvent site) {
        this.setType(type);
        this.setSite(site);
    }

    public static final Comparator<AbnormalPoint> compareByImportance = (ab1, ab2) -> {
        if (ab1.getType() != ab2.getType()) {
            return ab1.getType().getOrdinal() - ab2.getType().getOrdinal();
        }
        return 0;
    };

    public void generateDefaultSuggestions(GCModel model) {
        this.setDefaultSuggestions(new DefaultSuggestionGenerator(model, this).generate());
    }

    public AbnormalPointVO toVO() {
        AbnormalPointVO vo = new AbnormalPointVO();
        vo.setType(getType().getName());
        vo.setDefaultSuggestions(getDefaultSuggestions());
        return vo;
    }

    @Override
    public String toString() {
        return "AbnormalPoint{" +
                "type=" + getType() +
                ", defaultSuggestions=" + getDefaultSuggestions() +
                '}';
    }

    /**
     * @return the site
     */
    public TimedEvent getSite() {
        return site;
    }

    /**
     * @param site the site to set
     */
    public void setSite(TimedEvent site) {
        this.site = site;
    }

    /**
     * @return the type
     */
    public AbnormalType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(AbnormalType type) {
        this.type = type;
    }

    /**
     * @return the defaultSuggestions
     */
    public List<I18nStringView> getDefaultSuggestions() {
        return defaultSuggestions;
    }

    /**
     * @param defaultSuggestions the defaultSuggestions to set
     */
    public void setDefaultSuggestions(List<I18nStringView> defaultSuggestions) {
        this.defaultSuggestions = defaultSuggestions;
    }

    public static class AbnormalPointVO {
        // don't use I18nStringView because frontend need to check this field
        private String type;
        private List<I18nStringView> defaultSuggestions;
        /**
         * @return the type
         */
        public String getType() {
            return type;
        }
        /**
         * @param type the type to set
         */
        public void setType(String type) {
            this.type = type;
        }
        /**
         * @return the defaultSuggestions
         */
        public List<I18nStringView> getDefaultSuggestions() {
            return defaultSuggestions;
        }
        /**
         * @param defaultSuggestions the defaultSuggestions to set
         */
        public void setDefaultSuggestions(List<I18nStringView> defaultSuggestions) {
            this.defaultSuggestions = defaultSuggestions;
        }
    }
}
