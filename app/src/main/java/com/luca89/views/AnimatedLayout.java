/*
 * Copyright (C) 2016 Luca Di Maio <luca.dimaio1@gmail.com>
 *
 * This file is part of ThunderMusic Player.
 *
 * ThunderMusic is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * ThunderMusic is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.luca89.views;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

/**
 * Simple RelativeLayout wrapper to animate visibility changes
 */
public class AnimatedLayout extends RelativeLayout {

    public AnimatedLayout(Context context) {
        super(context);
        // 
    }

    public AnimatedLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 
    }

    public AnimatedLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // 
    }

    public boolean isVisible() {
        return getVisibility() == View.VISIBLE;
    }

    public void setVisibility(int visibility, boolean animate, int animation) {
        if (animate)
            if (visibility == VISIBLE) {
                slideIn(animation);
                setVisibility(visibility);
            } else if ((visibility == INVISIBLE) || (visibility == GONE)) {
                slideOut(animation);
                setVisibility(visibility);
            } else
                setVisibility(visibility);
    }

    public void slideOut(int animation) {
        this.setVisibility(View.GONE);
        Animation slide = AnimationUtils.loadAnimation(getContext(), animation);
        this.startAnimation(slide);

    }

    public void slideIn(int animation) {
        this.setVisibility(View.VISIBLE);
        Animation slide = AnimationUtils.loadAnimation(getContext(), animation);
        this.startAnimation(slide);
    }

}
