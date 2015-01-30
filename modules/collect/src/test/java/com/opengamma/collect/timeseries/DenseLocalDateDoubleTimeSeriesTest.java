/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.timeseries;

import static com.opengamma.collect.timeseries.DenseLocalDateDoubleTimeSeries.DenseTimeSeriesCalculation.INCLUDE_WEEKENDS;
import static com.opengamma.collect.timeseries.DenseLocalDateDoubleTimeSeries.DenseTimeSeriesCalculation.SKIP_WEEKENDS;
import static com.opengamma.collect.timeseries.LocalDateDoubleTimeSeries.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Doubles;
import com.opengamma.collect.Guavate;
import com.opengamma.collect.TestHelper;

@Test
public class DenseLocalDateDoubleTimeSeriesTest {

  private static final double TOLERANCE = 1e-5;

  private static final LocalDate DATE_2015_06_01 = date(2015, 6, 1);
  private static final LocalDate DATE_2014_06_01 = date(2014, 6, 1);
  private static final LocalDate DATE_2012_06_01 = date(2012, 6, 1);
  private static final LocalDate DATE_2010_06_01 = date(2010, 6, 1);
  private static final LocalDate DATE_2011_06_01 = date(2011, 6, 1);
  private static final LocalDate DATE_2013_06_01 = date(2013, 6, 1);
  private static final LocalDate DATE_2010_01_01 = date(2010, 1, 1);
  private static final LocalDate DATE_2010_01_02 = date(2010, 1, 2);
  private static final LocalDate DATE_2010_01_03 = date(2010, 1, 3);
  // Avoid weekends for these days
  private static final LocalDate DATE_2011_01_01 = date(2011, 1, 1);
  private static final LocalDate DATE_2012_01_01 = date(2012, 1, 1);
  private static final LocalDate DATE_2013_01_01 = date(2013, 1, 1);

  private static final LocalDate DATE_2014_01_01 = date(2014, 1, 1);
  private static final LocalDate DATE_2014_01_02 = date(2014, 1, 2);
  private static final LocalDate DATE_2014_01_03 = date(2014, 1, 3);

  private static final LocalDate DATE_2015_01_02 = date(2015, 1, 2);
  private static final LocalDate DATE_2015_01_05 = date(2015, 1, 5);
  private static final LocalDate DATE_2015_01_06 = date(2015, 1, 6);
  private static final LocalDate DATE_2015_01_07 = date(2015, 1, 7);
  private static final LocalDate DATE_2015_01_08 = date(2015, 1, 8);
  private static final LocalDate DATE_2015_01_09 = date(2015, 1, 9);
  private static final LocalDate DATE_2015_01_12 = date(2015, 1, 12);

  private static final ImmutableList<LocalDate> DATES_2015_1_WEEK = dates(
      DATE_2015_01_05, DATE_2015_01_06, DATE_2015_01_07, DATE_2015_01_08, DATE_2015_01_09);

  private static final ImmutableList<Double> VALUES_1_WEEK = values(10, 11, 12, 13, 14);

  private static final ImmutableList<LocalDate> DATES_2010_12 = dates(
      DATE_2010_01_01, DATE_2011_01_01, DATE_2012_01_01);
  private static final ImmutableList<Double> VALUES_10_12 = values(10, 11, 12);

  //-------------------------------------------------------------------------
  public void test_of_singleton() {
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.of(DATE_2011_01_01, 2d);
    assertEquals(test.isEmpty(), false);
    assertEquals(test.size(), 1);

    // Check start is not weekend

    assertEquals(test.containsDate(DATE_2010_01_01), false);
    assertEquals(test.containsDate(DATE_2011_01_01), true);
    assertEquals(test.containsDate(DATE_2012_01_01), false);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.empty());
    assertEquals(test.get(DATE_2011_01_01), OptionalDouble.of(2d));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.empty());
    assertEquals(test.dates().toArray(), new Object[]{DATE_2011_01_01});
    assertEquals(test.values().toArray(), new double[]{2d});
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_singleton_nullDateDisallowed() {
    LocalDateDoubleTimeSeries.of(null, 1d);
  }

  //-------------------------------------------------------------------------
  public void test_of_collectionCollection() {
    Collection<LocalDate> dates = dates(DATE_2011_01_01, DATE_2012_01_01);
    Collection<Double> values = values(2d, 3d);

    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.builder().putAll(dates, values).build();
    assertEquals(test.isEmpty(), false);
    assertEquals(test.size(), 2);
    assertEquals(test.containsDate(DATE_2010_01_01), false);
    assertEquals(test.containsDate(DATE_2011_01_01), true);
    assertEquals(test.containsDate(DATE_2012_01_01), true);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.empty());
    assertEquals(test.get(DATE_2011_01_01), OptionalDouble.of(2d));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(3d));
    assertEquals(test.dates().toArray(), new Object[]{DATE_2011_01_01, DATE_2012_01_01});
    assertEquals(test.values().toArray(), new double[]{2d, 3d});
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_dateCollectionNull() {
    Collection<Double> values = values(2d, 3d);

    LocalDateDoubleTimeSeries.builder().putAll(null, values).build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_valueCollectionNull() {
    Collection<LocalDate> dates = dates(DATE_2011_01_01, DATE_2012_01_01);

    LocalDateDoubleTimeSeries.builder().putAll(dates, null).build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_dateCollectionWithNull() {
    Collection<LocalDate> dates = Arrays.asList(DATE_2011_01_01, null);
    Collection<Double> values = values(2d, 3d);

    LocalDateDoubleTimeSeries.builder().putAll(dates, values).build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_valueCollectionWithNull() {
    Collection<LocalDate> dates = dates(DATE_2011_01_01, DATE_2012_01_01);
    Collection<Double> values = Arrays.asList(2d, null);

    LocalDateDoubleTimeSeries.builder().putAll(dates, values).build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_collectionsOfDifferentSize() {
    Collection<LocalDate> dates = dates(DATE_2011_01_01);
    Collection<Double> values = values(2d, 3d);

    LocalDateDoubleTimeSeries.builder().putAll(dates, values).build();
  }

  public void test_of_collectionCollection_datesUnordered() {
    Collection<LocalDate> dates = dates(DATE_2012_01_01, DATE_2011_01_01);
    Collection<Double> values = values(2d, 1d);

    LocalDateDoubleTimeSeries series = LocalDateDoubleTimeSeries.builder().putAll(dates, values).build();
    assertEquals(series.get(DATE_2011_01_01), OptionalDouble.of(1d));
    assertEquals(series.get(DATE_2012_01_01), OptionalDouble.of(2d));
  }

  //-------------------------------------------------------------------------
  public void test_of_map() {
    Map<LocalDate, Double> map = new HashMap<>();
    map.put(DATE_2011_01_01, 2d);
    map.put(DATE_2012_01_01, 3d);

    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.builder().putAll(map).build();
    assertEquals(test.isEmpty(), false);
    assertEquals(test.size(), 2);
    assertEquals(test.containsDate(DATE_2010_01_01), false);
    assertEquals(test.containsDate(DATE_2011_01_01), true);
    assertEquals(test.containsDate(DATE_2012_01_01), true);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.empty());
    assertEquals(test.get(DATE_2011_01_01), OptionalDouble.of(2d));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(3d));
    assertEquals(test.dates().toArray(), new Object[]{DATE_2011_01_01, DATE_2012_01_01});
    assertEquals(test.values().toArray(), new double[]{2d, 3d});
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_map_null() {

    LocalDateDoubleTimeSeries.builder().putAll((Map<LocalDate, Double>) null).build();
  }

  public void test_of_map_empty() {

    LocalDateDoubleTimeSeries series = LocalDateDoubleTimeSeries.builder().putAll(ImmutableMap.of()).build();
    assertEquals(series, empty());
  }


  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_map_dateNull() {
    Map<LocalDate, Double> map = new HashMap<>();
    map.put(DATE_2011_01_01, 2d);
    map.put(null, 3d);

    LocalDateDoubleTimeSeries.builder().putAll(map).build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_map_valueNull() {
    Map<LocalDate, Double> map = new HashMap<>();
    map.put(DATE_2011_01_01, 2d);
    map.put(DATE_2012_01_01, null);

    LocalDateDoubleTimeSeries.builder().putAll(map).build();
  }

  //-------------------------------------------------------------------------
  public void test_of_collection() {
    Collection<LocalDateDoublePoint> points = Arrays.asList(
        LocalDateDoublePoint.of(DATE_2011_01_01, 2d),
        LocalDateDoublePoint.of(DATE_2012_01_01, 3d));

    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.builder().putAll(points.stream()).build();
    assertEquals(test.isEmpty(), false);
    assertEquals(test.size(), 2);
    assertEquals(test.containsDate(DATE_2010_01_01), false);
    assertEquals(test.containsDate(DATE_2011_01_01), true);
    assertEquals(test.containsDate(DATE_2012_01_01), true);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.empty());
    assertEquals(test.get(DATE_2011_01_01), OptionalDouble.of(2d));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(3d));
    assertEquals(test.dates().toArray(), new Object[]{DATE_2011_01_01, DATE_2012_01_01});
    assertEquals(test.values().toArray(), new double[]{2d, 3d});
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collection_collectionNull() {

    LocalDateDoubleTimeSeries.builder().putAll((List<LocalDateDoublePoint>) null).build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collection_collectionWithNull() {
    Collection<LocalDateDoublePoint> points = Arrays.asList(
        LocalDateDoublePoint.of(DATE_2011_01_01, 2d), null);

    LocalDateDoubleTimeSeries.builder().putAll(points.stream()).build();
  }

  public void test_of_collection_empty() {
    Collection<LocalDateDoublePoint> points = ImmutableList.of();

    assertEquals(LocalDateDoubleTimeSeries.builder().putAll(points.stream()).build(), empty());
  }

  //-------------------------------------------------------------------------
  public void test_immutableViaBeanBuilder() {
    LocalDate startDate = DATE_2010_01_01;
    double[] values = {6, 5, 4};
    BeanBuilder<? extends DenseLocalDateDoubleTimeSeries> builder = DenseLocalDateDoubleTimeSeries.meta().builder();
    builder.set("startDate", startDate);
    builder.set("points", values);
    builder.set("dateCalculation", INCLUDE_WEEKENDS);
    DenseLocalDateDoubleTimeSeries test = builder.build();
    values[0] = -1;

    LocalDateDoublePoint[] points = test.stream().toArray(LocalDateDoublePoint[]::new);
    assertEquals(points[0], LocalDateDoublePoint.of(DATE_2010_01_01, 6d));
    assertEquals(points[1], LocalDateDoublePoint.of(DATE_2010_01_02, 5d));
    assertEquals(points[2], LocalDateDoublePoint.of(DATE_2010_01_03, 4d));
  }

  public void test_immutableValuesViaBeanGet() {

    LocalDateDoubleTimeSeries test =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    double[] array = (double[]) test.property("points").get();
    array[0] = -1;
    LocalDateDoublePoint[] points = test.stream().toArray(LocalDateDoublePoint[]::new);
    assertEquals(points[0], LocalDateDoublePoint.of(DATE_2015_01_05, 10d));
    assertEquals(points[1], LocalDateDoublePoint.of(DATE_2015_01_06, 11d));
    assertEquals(points[2], LocalDateDoublePoint.of(DATE_2015_01_07, 12d));
  }

  //-------------------------------------------------------------------------
  public void test_earliestLatest() {

    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.builder().putAll(DATES_2010_12, VALUES_10_12).build();
    assertEquals(test.getEarliestDate(), DATE_2010_01_01);
    assertEquals(test.getEarliestValue(), 10d, TOLERANCE);
    assertEquals(test.getLatestDate(), DATE_2012_01_01);
    assertEquals(test.getLatestValue(), 12d, TOLERANCE);
  }

  public void test_earliestLatest_whenEmpty() {
    LocalDateDoubleTimeSeries test = empty();
    TestHelper.assertThrows(test::getEarliestDate, NoSuchElementException.class);
    TestHelper.assertThrows(test::getEarliestValue, NoSuchElementException.class);
    TestHelper.assertThrows(test::getLatestDate, NoSuchElementException.class);
    TestHelper.assertThrows(test::getLatestValue, NoSuchElementException.class);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "subSeries")
  Object[][] data_subSeries() {
    return new Object[][] {
        // start = end -> empty
        {DATE_2011_01_01, DATE_2011_01_01, new int[] {}},
        // no overlap
        {date(2006, 1, 1), date(2009, 1, 1), new int[] {}},
        // single point
        {DATE_2015_01_06, DATE_2015_01_07, new int[] {1}},
        // include when start matches base, exclude when end matches base
        {DATE_2015_01_06, DATE_2015_01_08, new int[] {1, 2}},
        // include when start matches base
        {DATE_2015_01_05, DATE_2015_01_09, new int[] {0, 1, 2, 3}},
        // neither start nor end match
        {date(2014, 12, 31), date(2015, 2, 1), new int[] {0, 1, 2, 3, 4}},
    };
  }

  @Test(dataProvider = "subSeries")
  public void test_subSeries(LocalDate start, LocalDate end, int[] expected) {

    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder()
            .putAll(DATES_2015_1_WEEK, VALUES_1_WEEK)
            .build();

    LocalDateDoubleTimeSeries test = base.subSeries(start, end);

    assertEquals(test.size(), expected.length);
    for (int i = 0; i < DATES_2015_1_WEEK.size(); i++) {
      if (Arrays.binarySearch(expected, i) >= 0) {
        assertEquals(test.get(DATES_2015_1_WEEK.get(i)), OptionalDouble.of(VALUES_1_WEEK.get(i)));
      } else {
        assertEquals(test.get(DATES_2015_1_WEEK.get(i)), OptionalDouble.empty());
      }
    }
  }

  @Test(dataProvider = "subSeries")
  public void test_subSeries_emptySeries(LocalDate start, LocalDate end, int[] expected) {
    LocalDateDoubleTimeSeries test = empty().subSeries(start, end);
    assertEquals(test.size(), 0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_subSeries_startAfterEnd() {

    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    base.subSeries(date(2011, 1, 2), DATE_2011_01_01);
  }

  public void test_subSeries_picks_valid_dates() {

    LocalDateDoubleTimeSeries series = LocalDateDoubleTimeSeries.builder()
        .put(DATE_2015_01_02, 10)  // Friday
        .put(DATE_2015_01_05, 11)  // Mon
        .put(DATE_2015_01_06, 12)
        .put(DATE_2015_01_07, 13)
        .put(DATE_2015_01_08, 14)
        .put(DATE_2015_01_09, 15)  // Fri
        .put(DATE_2015_01_12, 16)  // Mon
        .build();

    // Pick using weekend dates
    LocalDateDoubleTimeSeries subSeries = series.subSeries(date(2015, 1, 4), date(2015, 1, 10));

    assertEquals(subSeries.size(), 5);
    assertEquals(subSeries.get(DATE_2015_01_02), OptionalDouble.empty());
    assertEquals(subSeries.get(date(2015, 1, 4)), OptionalDouble.empty());
    assertEquals(subSeries.get(DATE_2015_01_05), OptionalDouble.of(11));
    assertEquals(subSeries.get(DATE_2015_01_06), OptionalDouble.of(12));
    assertEquals(subSeries.get(DATE_2015_01_07), OptionalDouble.of(13));
    assertEquals(subSeries.get(DATE_2015_01_08), OptionalDouble.of(14));
    assertEquals(subSeries.get(DATE_2015_01_09), OptionalDouble.of(15));
    assertEquals(subSeries.get(DATE_2015_01_12), OptionalDouble.empty());
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "headSeries")
  Object[][] data_headSeries() {
    return new Object[][] {
        {0, new int[] {}},
        {1, new int[] {0}},
        {2, new int[] {0, 1}},
        {3, new int[] {0, 1, 2}},
        {4, new int[] {0, 1, 2, 3}},
        {5, new int[] {0, 1, 2, 3, 4}},
        {6, new int[] {0, 1, 2, 3, 4}},
    };
  }

  @Test(dataProvider = "headSeries")
  public void test_headSeries(int count, int[] expected) {

    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    LocalDateDoubleTimeSeries test = base.headSeries(count);
    assertEquals(test.size(), expected.length);
    for (int i = 0; i < DATES_2015_1_WEEK.size(); i++) {
      if (Arrays.binarySearch(expected, i) >= 0) {
        assertEquals(test.get(DATES_2015_1_WEEK.get(i)), OptionalDouble.of(VALUES_1_WEEK.get(i)));
      } else {
        assertEquals(test.get(DATES_2015_1_WEEK.get(i)), OptionalDouble.empty());
      }
    }
  }

  @Test(dataProvider = "headSeries")
  public void test_headSeries_emptySeries(int count, int[] expected) {
    LocalDateDoubleTimeSeries test = empty().headSeries(count);
    assertEquals(test.size(), 0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_headSeries_negative() {

    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    base.headSeries(-1);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "tailSeries")
  Object[][] data_tailSeries() {
    return new Object[][] {
        {0, new int[] {}},
        {1, new int[] {4}},
        {2, new int[] {3, 4}},
        {3, new int[] {2, 3, 4}},
        {4, new int[] {1, 2, 3, 4}},
        {5, new int[] {0, 1, 2, 3, 4}},
        {6, new int[] {0, 1, 2, 3, 4}},
    };
  }

  @Test(dataProvider = "tailSeries")
  public void test_tailSeries(int count, int[] expected) {

    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    LocalDateDoubleTimeSeries test = base.tailSeries(count);
    assertEquals(test.size(), expected.length);
    for (int i = 0; i < DATES_2015_1_WEEK.size(); i++) {
      if (Arrays.binarySearch(expected, i) >= 0) {
        assertEquals(test.get(DATES_2015_1_WEEK.get(i)), OptionalDouble.of(VALUES_1_WEEK.get(i)));
      } else {
        assertEquals(test.get(DATES_2015_1_WEEK.get(i)), OptionalDouble.empty());
      }
    }
  }

  @Test(dataProvider = "tailSeries")
  public void test_tailSeries_emptySeries(int count, int[] expected) {
    LocalDateDoubleTimeSeries test = empty().tailSeries(count);
    assertEquals(test.size(), 0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_tailSeries_negative() {

    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    base.tailSeries(-1);
  }

  //-------------------------------------------------------------------------
  public void test_stream() {

    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2010_12, VALUES_10_12).build();
    Object[] test = base.stream().toArray();
    assertEquals(test[0], LocalDateDoublePoint.of(DATE_2010_01_01, 10));
    assertEquals(test[1], LocalDateDoublePoint.of(DATE_2011_01_01, 11));
    assertEquals(test[2], LocalDateDoublePoint.of(DATE_2012_01_01, 12));
  }

  public void test_stream_withCollector() {

    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2010_12, VALUES_10_12).build();
    LocalDateDoubleTimeSeries test = base.stream()
        .map(point -> point.withValue(1.5d))
        .collect(LocalDateDoubleTimeSeries.collector());
    assertEquals(test.size(), 3);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.of(1.5));
    assertEquals(test.get(DATE_2011_01_01), OptionalDouble.of(1.5));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(1.5));
  }

  //-------------------------------------------------------------------------
  public void test_dateStream() {

    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.builder().putAll(DATES_2010_12, VALUES_10_12).build();
    LocalDate[] test = base.dates().toArray(LocalDate[]::new);
    assertEquals(test[0], DATE_2010_01_01);
    assertEquals(test[1], DATE_2011_01_01);
    assertEquals(test[2], DATE_2012_01_01);
  }

  public void test_valueStream() {

    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2010_12, VALUES_10_12).build();
    double[] test = base.values().toArray();
    assertEquals(test[0], 10, TOLERANCE);
    assertEquals(test[1], 11, TOLERANCE);
    assertEquals(test[2], 12, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  public void test_forEach() {

    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    AtomicInteger counter = new AtomicInteger();
    base.forEach((date, value) -> counter.addAndGet((int) value));
    assertEquals(counter.get(), 10 + 11 + 12 + 13 + 14);
  }

  //-------------------------------------------------------------------------
  public void test_combineWith_intersectionWithNoMatchingElements() {

    LocalDateDoubleTimeSeries series1 =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();

    List<LocalDate> dates2 = dates(DATE_2010_06_01, DATE_2011_06_01, DATE_2012_06_01, DATE_2013_06_01, DATE_2014_06_01);

    LocalDateDoubleTimeSeries series2 =
        LocalDateDoubleTimeSeries.builder().putAll(dates2, VALUES_1_WEEK).build();

    LocalDateDoubleTimeSeries test = series1.combineWith(series2, (l, r) -> l + r);
    assertEquals(test, SparseLocalDateDoubleTimeSeries.EMPTY_SERIES);
  }

  public void test_combineWith_intersectionWithSomeMatchingElements() {

    LocalDateDoubleTimeSeries series1 =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();

    Map<LocalDate, Double> updates = ImmutableMap.of(
        DATE_2015_01_02, 1.0,
        DATE_2015_01_05, 1.1,
        DATE_2015_01_08, 1.2,
        DATE_2015_01_09, 1.3,
        DATE_2015_01_12, 1.4);

    LocalDateDoubleTimeSeries series2 =
        LocalDateDoubleTimeSeries.builder()
            .putAll(updates)
            .build();

    LocalDateDoubleTimeSeries test = series1.combineWith(series2, (l, r) -> l + r);
    assertEquals(test.size(), 3);
    assertEquals(test.get(DATE_2015_01_05), OptionalDouble.of(11.1));
    assertEquals(test.get(DATE_2015_01_08), OptionalDouble.of(14.2));
    assertEquals(test.get(DATE_2015_01_09), OptionalDouble.of(15.3));
  }

  public void test_combineWith_intersectionWithSomeMatchingElements2() {
    List<LocalDate> dates1 = dates(DATE_2010_01_01, DATE_2011_01_01, DATE_2012_01_01, DATE_2014_01_01, DATE_2015_06_01);
    List<Double> values1 = values(10, 11, 12, 13, 14);

    LocalDateDoubleTimeSeries series1 = LocalDateDoubleTimeSeries.builder().putAll(dates1, values1).build();

    List<LocalDate> dates2 = dates(DATE_2010_01_01, DATE_2011_06_01, DATE_2012_01_01, DATE_2013_01_01, DATE_2014_01_01);
    List<Double> values2 = values(1.0, 1.1, 1.2, 1.3, 1.4);

    LocalDateDoubleTimeSeries series2 = LocalDateDoubleTimeSeries.builder().putAll(dates2, values2).build();

    LocalDateDoubleTimeSeries test = series1.combineWith(series2, (l, r) -> l + r);
    assertEquals(test.size(), 3);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.of(11.0));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(13.2));
    assertEquals(test.get(DATE_2014_01_01), OptionalDouble.of(14.4));
  }

  public void test_combineWith_intersectionWithAllMatchingElements() {
    List<LocalDate> dates1 = DATES_2015_1_WEEK;
    List<Double> values1 = values(10, 11, 12, 13, 14);

    LocalDateDoubleTimeSeries series1 =
        LocalDateDoubleTimeSeries.builder().putAll(dates1, values1).build();
    List<LocalDate> dates2 = DATES_2015_1_WEEK;
    List<Double> values2 = values(1.0, 1.1, 1.2, 1.3, 1.4);

    LocalDateDoubleTimeSeries series2 =
        LocalDateDoubleTimeSeries.builder().putAll(dates2, values2).build();

    LocalDateDoubleTimeSeries combined = series1.combineWith(series2, (l, r) -> l + r);
    assertEquals(combined.size(), 5);
    assertEquals(combined.getEarliestDate(), DATE_2015_01_05);
    assertEquals(combined.getLatestDate(), DATE_2015_01_09);
    assertEquals(combined.get(DATE_2015_01_05), OptionalDouble.of(11.0));
    assertEquals(combined.get(DATE_2015_01_06), OptionalDouble.of(12.1));
    assertEquals(combined.get(DATE_2015_01_07), OptionalDouble.of(13.2));
    assertEquals(combined.get(DATE_2015_01_08), OptionalDouble.of(14.3));
    assertEquals(combined.get(DATE_2015_01_09), OptionalDouble.of(15.4));
  }

  //-------------------------------------------------------------------------
  public void test_mapValues_addConstantToSeries() {

    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    LocalDateDoubleTimeSeries test = base.mapValues(d -> d + 5);
    List<Double> expectedValues = values(15, 16, 17, 18, 19);

    assertEquals(test, LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, expectedValues).build());
  }

  public void test_mapValues_multiplySeries() {

    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();

    LocalDateDoubleTimeSeries test = base.mapValues(d -> d * 5);
    List<Double> expectedValues = values(50, 55, 60, 65, 70);

    assertEquals(test, LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, expectedValues).build());
  }

  public void test_mapValues_invertSeries() {
    List<Double> values = values(1, 2, 4, 5, 8);

    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, values).build();
    LocalDateDoubleTimeSeries test = base.mapValues(d -> 1 / d);
    List<Double> expectedValues = values(1, 0.5, 0.25, 0.2, 0.125);

    assertEquals(test, LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, expectedValues).build());
  }

  //-------------------------------------------------------------------------
  public void test_filter_byDate() {
    List<LocalDate> dates = dates(DATE_2010_01_01, DATE_2011_06_01, DATE_2012_01_01, DATE_2013_06_01, DATE_2014_01_01);

    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.builder().putAll(dates, VALUES_1_WEEK).build();
    LocalDateDoubleTimeSeries test = base.filter((ld, v) -> ld.getMonthValue() != 6);
    assertEquals(test.size(), 3);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.of(10d));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(12d));
    assertEquals(test.get(DATE_2014_01_01), OptionalDouble.of(14d));
  }

  public void test_filter_byValue() {

    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    LocalDateDoubleTimeSeries test = base.filter((ld, v) -> v % 2 == 1);
    assertEquals(test.size(), 2);
    assertEquals(test.get(DATE_2015_01_06), OptionalDouble.of(11d));
    assertEquals(test.get(DATE_2015_01_08), OptionalDouble.of(13d));
  }

  public void test_filter_byDateAndValue() {
    List<LocalDate> dates = dates(DATE_2010_01_01, DATE_2011_06_01, DATE_2012_01_01, DATE_2013_06_01, DATE_2014_01_01);

    LocalDateDoubleTimeSeries series = LocalDateDoubleTimeSeries.builder().putAll(dates, VALUES_1_WEEK).build();

    LocalDateDoubleTimeSeries test = series.filter((ld, v) -> ld.getYear() >= 2012 && v % 2 == 0);
    assertEquals(test.size(), 2);
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(12d));
    assertEquals(test.get(DATE_2014_01_01), OptionalDouble.of(14d));
  }

  //-------------------------------------------------------------------------
  public void test_equals_similarSeriesAreEqual() {
    LocalDateDoubleTimeSeries series1 = LocalDateDoubleTimeSeries.of(DATE_2014_01_01, 1d);

    LocalDateDoubleTimeSeries series2 = LocalDateDoubleTimeSeries.builder().putAll(dates(DATE_2014_01_01), values(1d)).build();
    assertEquals(series1.size(), 1);
    assertEquals(series1, series2);
    assertEquals(series1, series1);
    assertEquals(series1.hashCode(), series1.hashCode());
  }

  public void test_equals_notEqual() {
    LocalDateDoubleTimeSeries series1 = LocalDateDoubleTimeSeries.of(DATE_2014_01_01, 1d);
    LocalDateDoubleTimeSeries series2 = LocalDateDoubleTimeSeries.of(DATE_2013_06_01, 1d);
    LocalDateDoubleTimeSeries series3 = LocalDateDoubleTimeSeries.of(DATE_2014_01_01, 3d);
    assertNotEquals(series1, series2);
    assertNotEquals(series1, series3);
  }

  public void test_equals_bad() {
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.of(DATE_2014_01_01, 1d);
    assertEquals(test.equals(""), false);
    assertEquals(test.equals(null), false);
  }

  public void checkOffsetsIncludeWeekends() {

    Map<LocalDate, Double> map = ImmutableMap.<LocalDate, Double>builder()
        .put(dt(2014, 12, 26), 14d)
            // Weekend
        .put(dt(2014, 12, 29), 13d)
        .put(dt(2014, 12, 30), 12d)
        .put(dt(2014, 12, 31), 11d)
            // 1st is bank hol so no data
        .put(dt(2015, 1, 2), 11d)
            // Weekend, so no data
        .put(dt(2015, 1, 5), 12d)
        .put(dt(2015, 1, 6), 13d)
        .put(dt(2015, 1, 7), 14d)
        .build();

    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.builder().putAll(map).build();
    assertThat(ts.get(dt(2014, 12, 26))).isEqualTo(OptionalDouble.of(14d));
    assertThat(ts.get(dt(2014, 12, 27))).isEqualTo(OptionalDouble.empty());
    assertThat(ts.get(dt(2014, 12, 28))).isEqualTo(OptionalDouble.empty());
    assertThat(ts.get(dt(2014, 12, 29))).isEqualTo(OptionalDouble.of(13d));
    assertThat(ts.get(dt(2014, 12, 30))).isEqualTo(OptionalDouble.of(12d));
    assertThat(ts.get(dt(2014, 12, 31))).isEqualTo(OptionalDouble.of(11d));
    assertThat(ts.get(dt(2015, 1, 1))).isEqualTo(OptionalDouble.empty());
    assertThat(ts.get(dt(2015, 1, 2))).isEqualTo(OptionalDouble.of(11d));
    assertThat(ts.get(dt(2015, 1, 3))).isEqualTo(OptionalDouble.empty());
    assertThat(ts.get(dt(2015, 1, 4))).isEqualTo(OptionalDouble.empty());
    assertThat(ts.get(dt(2015, 1, 5))).isEqualTo(OptionalDouble.of(12d));
    assertThat(ts.get(dt(2015, 1, 6))).isEqualTo(OptionalDouble.of(13d));
    assertThat(ts.get(dt(2015, 1, 7))).isEqualTo(OptionalDouble.of(14d));
  }

  //-------------------------------------------------------------------------
  public void test_coverage() {
    TestHelper.coverImmutableBean(
        DenseLocalDateDoubleTimeSeries.of(DATE_2015_01_05, DATE_2015_01_05, Stream.of(LocalDateDoublePoint.of(DATE_2015_01_05, 1d)), SKIP_WEEKENDS));
  }

  //-------------------------------------------------------------------------
  private static LocalDate date(int year, int month, int day) {
    return LocalDate.of(year, month, day);
  }

  private static ImmutableList<LocalDate> dates(LocalDate... dates) {
    return ImmutableList.copyOf(dates);
  }

  
  private static ImmutableList<Double> values(double... values) {
    return ImmutableList.copyOf(Doubles.asList(values));
  }

  public void checkOffsetsSkipWeekends() {

    Map<LocalDate, Double> map = ImmutableMap.<LocalDate, Double>builder()
        .put(dt(2014, 12, 26), 14d)
            // Weekend
        .put(dt(2014, 12, 29), 13d)
        .put(dt(2014, 12, 30), 12d)
        .put(dt(2014, 12, 31), 11d)
        .put(dt(2015, 1, 1), Double.NaN)
        .put(dt(2015, 1, 2), 11d)
            // Weekend, so no data
        .put(dt(2015, 1, 5), 12d)
        .put(dt(2015, 1, 6), 13d)
        .put(dt(2015, 1, 7), 14d)
        .build();

    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.builder().putAll(map).build();
    assertThat(ts.get(dt(2014, 12, 26))).isEqualTo(OptionalDouble.of(14d));
    assertThat(ts.get(dt(2014, 12, 29))).isEqualTo(OptionalDouble.of(13d));
    assertThat(ts.get(dt(2014, 12, 30))).isEqualTo(OptionalDouble.of(12d));
    assertThat(ts.get(dt(2014, 12, 31))).isEqualTo(OptionalDouble.of(11d));
    assertThat(ts.get(dt(2015, 1, 1))).isEqualTo(OptionalDouble.empty());
    assertThat(ts.get(dt(2015, 1, 2))).isEqualTo(OptionalDouble.of(11d));
    assertThat(ts.get(dt(2015, 1, 3))).isEqualTo(OptionalDouble.empty());
    assertThat(ts.get(dt(2015, 1, 4))).isEqualTo(OptionalDouble.empty());
    assertThat(ts.get(dt(2015, 1, 5))).isEqualTo(OptionalDouble.of(12d));
    assertThat(ts.get(dt(2015, 1, 6))).isEqualTo(OptionalDouble.of(13d));
    assertThat(ts.get(dt(2015, 1, 7))).isEqualTo(OptionalDouble.of(14d));
  }

  public void underOneWeekNoWeekend() {

    Map<LocalDate, Double> map = ImmutableMap.<LocalDate, Double>builder()
        .put(dt(2015, 1, 5), 12d) // Monday
        .put(dt(2015, 1, 6), 13d)
        .put(dt(2015, 1, 7), 14d)
        .put(dt(2015, 1, 8), 15d)
        .put(dt(2015, 1, 9), 16d) // Friday
        .build();

    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.builder().putAll(map).build();
    assertThat(ts.get(dt(2015, 1, 5))).isEqualTo(OptionalDouble.of(12d));
    assertThat(ts.get(dt(2015, 1, 9))).isEqualTo(OptionalDouble.of(16d));
  }

  public void underOneWeekWithWeekend() {

    Map<LocalDate, Double> map = ImmutableMap.<LocalDate, Double>builder()
        .put(dt(2015, 1, 1), 10d) // Thursday
        .put(dt(2015, 1, 2), 11d) // Friday
        .put(dt(2015, 1, 5), 12d)
        .put(dt(2015, 1, 6), 13d)
        .put(dt(2015, 1, 7), 14d)
        .put(dt(2015, 1, 8), 15d) // Thursday
        .put(dt(2015, 1, 9), 16d) // Friday
        .build();

    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.builder().putAll(map).build();
    assertThat(ts.get(dt(2015, 1, 1))).isEqualTo(OptionalDouble.of(10d));
    assertThat(ts.get(dt(2015, 1, 2))).isEqualTo(OptionalDouble.of(11d));
    assertThat(ts.get(dt(2015, 1, 5))).isEqualTo(OptionalDouble.of(12d));
    assertThat(ts.get(dt(2015, 1, 6))).isEqualTo(OptionalDouble.of(13d));
    assertThat(ts.get(dt(2015, 1, 7))).isEqualTo(OptionalDouble.of(14d));
    assertThat(ts.get(dt(2015, 1, 8))).isEqualTo(OptionalDouble.of(15d));
    assertThat(ts.get(dt(2015, 1, 9))).isEqualTo(OptionalDouble.of(16d));
  }

  public void roundTrip() {
    Map<LocalDate, Double> in = ImmutableMap.<LocalDate, Double>builder()
        .put(dt(2015, 1, 1), 10d) // Thursday
        .put(dt(2015, 1, 2), 11d) // Friday
        .put(dt(2015, 1, 5), 12d)
        .put(dt(2015, 1, 6), 13d)
        .put(dt(2015, 1, 7), 14d)
        .put(dt(2015, 1, 8), 15d) // Thursday
        .put(dt(2015, 1, 9), 16d) // Friday
        .build();

    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.builder().putAll(in).build();

    Map<LocalDate, Double> out = ts.stream()
        .collect(Guavate.toImmutableMap(LocalDateDoublePoint::getDate, LocalDateDoublePoint::getValue));
    assertThat(out).isEqualTo(in);
  }

  private LocalDate dt(int yr, int mth, int day) {
    return LocalDate.of(yr, mth, day);
  }
}