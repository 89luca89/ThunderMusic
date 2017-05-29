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

package com.luca89.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.luca89.thundermusic.R;
import com.luca89.utils.ThemeUtils;

import java.util.List;

public class ShoutcastRadioAdapter extends BaseAdapter {
    // The list of artworks to display
    List<String> radio;
    // An inflator to use when creating rows
    private Context mContext;

    /**
     * @param context this is the context that the list will be shown in - used to
     *                create new list rows
     */
    public ShoutcastRadioAdapter(Context context,
                                 List<String> radio) {
        this.radio = radio;
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return radio.size();
    }

    @Override
    public Object getItem(int position) {
        return radio.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // If convertView wasn't null it means we have already set it to our
        // list_item_user_video so no need to do it again
        ViewHolder vh;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.track_list_item_simple, parent, false);
            vh = new ViewHolder();

            vh.thumb = (ImageView) convertView.findViewById(R.id.icon);
            vh.line = (TextView) convertView.findViewById(R.id.line1);
            vh.btn = (ImageView) convertView.findViewById(R.id.menu_button);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }
        // Unused MenuButton in this adapter
        vh.btn.setVisibility(View.GONE);

        if (position != 0)
            vh.thumb.setImageDrawable(ThemeUtils
                    .colorizeResourceDrawable(R.drawable.mixradio_holo_dark, mContext));
        else
            vh.thumb.setImageDrawable(ThemeUtils
                    .colorizeResourceDrawable(R.drawable.mixradio_search_holo_dark, mContext));

        vh.line.setText(radio.get(position));
        vh.line.setTextColor(ThemeUtils.getTextColor(mContext));

        return convertView;
    }

    static class ViewHolder {
        ImageView thumb;
        TextView line;
        ImageView btn;
    }
}