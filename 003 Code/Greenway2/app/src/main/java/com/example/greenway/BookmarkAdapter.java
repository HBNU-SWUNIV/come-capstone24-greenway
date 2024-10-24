package com.example.greenway;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import android.widget.ImageView;


public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {
    private List<Object> bookmarkList;
    private Context context;
    private OnBookmarkClickListener listener;
    private boolean isEditMode = false;
    private List<Object> selectedItems = new ArrayList<>();

    public BookmarkAdapter(Context context, List<Object> bookmarkList, OnBookmarkClickListener listener) {
        this.context = context;
        this.bookmarkList = bookmarkList;
        this.listener = listener;
    }

    public interface OnBookmarkClickListener {
        void onBookmarkClick(Object item);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bookmarker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = bookmarkList.get(position);

        if (item instanceof ParkingInfo) {
            holder.bindParkingInfo((ParkingInfo) item, position);
        } else if (item instanceof BikeInfo) {
            holder.bindBikeInfo((BikeInfo) item, position);
        } else if (item instanceof KickboardInfo) { // KickboardInfo 추가
            holder.bindKickboardInfo((KickboardInfo) item, position);
        }

        holder.itemView.setOnClickListener(v -> {
            if (isEditMode) {
                if (selectedItems.contains(item)) {
                    selectedItems.remove(item);
                    holder.itemView.setBackgroundColor(Color.WHITE);
                } else {
                    selectedItems.add(item);
                    holder.itemView.setBackgroundColor(Color.LTGRAY);
                }
            } else {
                listener.onBookmarkClick(item);
            }
        });

        holder.itemView.setBackgroundColor(selectedItems.contains(item) ? Color.LTGRAY : Color.WHITE);
    }

    @Override
    public int getItemCount() {
        return bookmarkList.size();
    }

    public void setEditMode(boolean editMode) {
        isEditMode = editMode;
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public List<Object> getSelectedItems() {
        return selectedItems;
    }

    public void removeSelectedItems() {
        bookmarkList.removeAll(selectedItems);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView parkingNameTextView;
        TextView parkingAddressTextView;
        ImageView bookmarkerButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            parkingNameTextView = itemView.findViewById(R.id.parking_name);
            parkingAddressTextView = itemView.findViewById(R.id.parking_address);
            bookmarkerButton = itemView.findViewById(R.id.bookmarker_button);
        }

        public void bindParkingInfo(ParkingInfo parkingInfo, int position) {
            parkingNameTextView.setText(parkingInfo.getParkingName());
            parkingAddressTextView.setText(parkingInfo.getAddr());

            if (Bookmarker.isParkingBookmarked(context, parkingInfo)) {
                bookmarkerButton.setImageResource(R.drawable.ic_filled_bookmark);
            } else {
                bookmarkerButton.setImageResource(R.drawable.ic_empty_bookmarker);
            }

            bookmarkerButton.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("즐겨찾기 해제")
                        .setMessage("즐겨찾기에서 삭제하겠습니까?")
                        .setPositiveButton("예", (dialog, which) -> {
                            Bookmarker.removeParking(context, parkingInfo);
                            bookmarkList.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, bookmarkList.size());
                            Toast.makeText(context, "즐겨찾기에서 제거되었습니다.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("아니오", null)
                        .show();
            });
        }

        public void bindBikeInfo(BikeInfo bikeInfo, int position) {
            parkingNameTextView.setText(bikeInfo.getStationName());
            parkingAddressTextView.setText(bikeInfo.getAddress());

            if (Bookmarker.isBikeBookmarked(context, bikeInfo)) {
                bookmarkerButton.setImageResource(R.drawable.ic_filled_bookmark);
            } else {
                bookmarkerButton.setImageResource(R.drawable.ic_empty_bookmarker);
            }

            bookmarkerButton.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("즐겨찾기 해제")
                        .setMessage("즐겨찾기에서 삭제하겠습니까?")
                        .setPositiveButton("예", (dialog, which) -> {
                            Bookmarker.removeBike(context, bikeInfo);
                            bookmarkList.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, bookmarkList.size());
                            Toast.makeText(context, "즐겨찾기에서 제거되었습니다.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("아니오", null)
                        .show();
            });
        }

        public void bindKickboardInfo(KickboardInfo kickboardInfo, int position) {
            parkingNameTextView.setText(kickboardInfo.getLocation());

            // boolean 값을 String으로 변환하여 TextView에 설정
            parkingAddressTextView.setText(kickboardInfo.isHasStand() ? "스탠드 있음" : "스탠드 없음");

            if (Bookmarker.isKickboardBookmarked(context, kickboardInfo)) {
                bookmarkerButton.setImageResource(R.drawable.ic_filled_bookmark);
            } else {
                bookmarkerButton.setImageResource(R.drawable.ic_empty_bookmarker);
            }

            bookmarkerButton.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("즐겨찾기 해제")
                        .setMessage("즐겨찾기에서 삭제하겠습니까?")
                        .setPositiveButton("예", (dialog, which) -> {
                            Bookmarker.removeKickboard(context, kickboardInfo);
                            bookmarkList.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, bookmarkList.size());
                            Toast.makeText(context, "즐겨찾기에서 제거되었습니다.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("아니오", null)
                        .show();
            });
        }

    }
}
