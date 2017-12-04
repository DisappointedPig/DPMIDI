package com.disappointedpig.dpmidi;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class GenericRecyclerViewAdapter<T extends ViewModel> extends RecyclerView.Adapter<GenericRecyclerViewAdapter.ViewHolder> {
    private OnFragmentInteractionListener mListener;

    private List<? extends T> list;

    public GenericRecyclerViewAdapter(List<? extends T> list, OnFragmentInteractionListener listener) {
        this.list = list;
        mListener = listener;
    }

    public static class ViewHolder<V extends ViewDataBinding> extends RecyclerView.ViewHolder implements View.OnClickListener  {
        private V v;
        private OnFragmentInteractionListener mListener;
        public ViewModel model;

        public ViewHolder(V v, OnFragmentInteractionListener listener) {
            super(v.getRoot());
            this.v = v;
            mListener = listener;

            if (mListener != null) {
                Log.d("ViewHolder", "mListner is not null");

                itemView.setOnClickListener(this);
            } else {
                Log.d("ViewHolder", "mListner is null");

            }

        }

        @Override
        public void onClick(View view) {
            Log.d("ViewHolder","click");
            if (mListener != null)
                mListener.onRecyclerItemClick(model);
        }

        public V getBinding() {
            return v;
        }
    }


    @Override
    public int getItemViewType(int position) {
        return list.get(position).layoutId();
    }

    @Override
    public GenericRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewDataBinding bind = DataBindingUtil.bind(LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false));

        return new ViewHolder<>(bind, mListener);
    }

    @Override
    public void onBindViewHolder(GenericRecyclerViewAdapter.ViewHolder holder, int position) {
        final T model = list.get(position);
        holder.getBinding().setVariable(BR.model, model);
        holder.model = model;

        holder.getBinding().executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


}
