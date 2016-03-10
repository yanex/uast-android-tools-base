/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.chartlib.model;

import com.android.annotations.NonNull;

import java.util.LinkedList;
import java.util.List;

/**
 * The data model used by a LineChart component
 */
public class LineChartData {

    @NonNull
    private final List<RangedContinuousSeries> mSeries = new LinkedList<RangedContinuousSeries>();

    public void add(@NonNull RangedContinuousSeries series) {
        mSeries.add(series);
    }

    @NonNull
    public List<RangedContinuousSeries> series() {
        return mSeries;
    }
}
