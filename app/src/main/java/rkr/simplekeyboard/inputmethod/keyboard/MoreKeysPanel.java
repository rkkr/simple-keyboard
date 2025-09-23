/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2017 Raimondas Rimkus
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

package rkr.simplekeyboard.inputmethod.keyboard;

import android.view.View;
import android.view.ViewGroup;

public interface MoreKeysPanel {
    interface Controller {
        /**
         * Add the {@link MoreKeysPanel} to the target view.
         * @param panel the panel to be shown.
         */
        void onShowMoreKeysPanel(final MoreKeysPanel panel);

        /**
         * Remove the current {@link MoreKeysPanel} from the target view.
         */
        void onDismissMoreKeysPanel();

        /**
         * Instructs the parent to cancel the panel (e.g., when entering a different input mode).
         */
        void onCancelMoreKeysPanel();
    }

    Controller EMPTY_CONTROLLER = new Controller() {
        @Override
        public void onShowMoreKeysPanel(final MoreKeysPanel panel) {}
        @Override
        public void onDismissMoreKeysPanel() {}
        @Override
        public void onCancelMoreKeysPanel() {}
    };

    /**
     * Initializes the layout and event handling of this {@link MoreKeysPanel} and calls the
     * controller's onShowMoreKeysPanel to add the panel's container view.
     *
     * @param parentView the parent view of this {@link MoreKeysPanel}
     * @param controller the controller that can dismiss this {@link MoreKeysPanel}
     * @param pointX x coordinate of this {@link MoreKeysPanel}
     * @param pointY y coordinate of this {@link MoreKeysPanel}
     * @param listener the listener that will receive keyboard action from this
     * {@link MoreKeysPanel}.
     */
    // TODO: Currently the MoreKeysPanel is inside a container view that is added to the parent.
    // Consider the simpler approach of placing the MoreKeysPanel itself into the parent view.
    void showMoreKeysPanel(View parentView, Controller controller, int pointX,
                           int pointY, KeyboardActionListener listener);

    /**
     * Dismisses the more keys panel and calls the controller's onDismissMoreKeysPanel to remove
     * the panel's container view.
     */
    void dismissMoreKeysPanel();

    /**
     * Process a move event on the more keys panel.
     *
     * @param x translated x coordinate of the touch point
     * @param y translated y coordinate of the touch point
     * @param pointerId pointer id touch point
     */
    void onMoveEvent(final int x, final int y, final int pointerId);

    /**
     * Process a down event on the more keys panel.
     *
     * @param x translated x coordinate of the touch point
     * @param y translated y coordinate of the touch point
     * @param pointerId pointer id touch point
     */
    void onDownEvent(final int x, final int y, final int pointerId);

    /**
     * Process an up event on the more keys panel.
     *
     * @param x translated x coordinate of the touch point
     * @param y translated y coordinate of the touch point
     * @param pointerId pointer id touch point
     */
    void onUpEvent(final int x, final int y, final int pointerId);

    /**
     * Translate X-coordinate of touch event to the local X-coordinate of this
     * {@link MoreKeysPanel}.
     *
     * @param x the global X-coordinate
     * @return the local X-coordinate to this {@link MoreKeysPanel}
     */
    int translateX(int x);

    /**
     * Translate Y-coordinate of touch event to the local Y-coordinate of this
     * {@link MoreKeysPanel}.
     *
     * @param y the global Y-coordinate
     * @return the local Y-coordinate to this {@link MoreKeysPanel}
     */
    int translateY(int y);

    /**
     * Show this {@link MoreKeysPanel} in the parent view.
     *
     * @param parentView the {@link ViewGroup} that hosts this {@link MoreKeysPanel}.
     */
    void showInParent(ViewGroup parentView);

    /**
     * Remove this {@link MoreKeysPanel} from the parent view.
     */
    void removeFromParent();

    /**
     * Return whether the panel is currently being shown.
     */
    boolean isShowingInParent();
}
