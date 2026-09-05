package com.android.internal.util.extra;

import java.util.Map;


/**
 * Interface for prop providers.
 *
 * This interface defines the methods that a prop provider must implement
 * to provide access to build properties.
 *
 * @hide
 */
public interface IPropProvider {

    /**
     * Checks if a valid set of props is available.
     *
     * @return true if a valid set of props is available, false otherwise
     * @hide
     */
    boolean hasProps();

    /**
     * Retrieves the PIH properties.
     *
     * @return the PIH properties as a Map
     * @hide
     */
    Map<String, String> getProps();
}
