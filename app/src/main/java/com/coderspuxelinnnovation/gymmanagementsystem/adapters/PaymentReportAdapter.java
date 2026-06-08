package com.coderspuxelinnnovation.gymmanagementsystem.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.coderspuxelinnnovation.gymmanagementsystem.R;
import com.coderspuxelinnnovation.gymmanagementsystem.models.PaymentReportModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PaymentReportAdapter extends RecyclerView.Adapter<PaymentReportAdapter.ViewHolder> {

    private Context context;
    private List<PaymentReportModel> paymentList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());

    public PaymentReportAdapter(Context context, List<PaymentReportModel> paymentList) {
        this.context = context;
        this.paymentList = paymentList;
    }

    public void updateList(List<PaymentReportModel> newList) {
        this.paymentList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_payment_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PaymentReportModel payment = paymentList.get(position);

        holder.tvSerial.setText(String.valueOf(position + 1));
        holder.tvMemberName.setText(payment.getMemberName());
        holder.tvAmount.setText("₹" + payment.getAmount());
        holder.tvPaymentMode.setText(payment.getPaymentMode());
        holder.tvDate.setText(dateFormat.format(new Date(payment.getPaymentDate())));
        holder.tvForMonth.setText(payment.getForMonth());
    }

    @Override
    public int getItemCount() {
        return paymentList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSerial, tvMemberName, tvAmount, tvPaymentMode, tvDate, tvForMonth;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSerial = itemView.findViewById(R.id.tvSerial);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvPaymentMode = itemView.findViewById(R.id.tvPaymentMode);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvForMonth = itemView.findViewById(R.id.tvForMonth);
        }
    }
}