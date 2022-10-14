package org.smsserver.viewModel;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class HttpViewModelFactory implements ViewModelProvider.Factory {

    private Context context;
    private SharedPreferences preferences;

    public HttpViewModelFactory(Context context, SharedPreferences preferences){
        this.context = context;
        this.preferences = preferences;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HttpViewModel.class)) {

            try {
                return (T) new HttpViewModel(context, preferences);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        throw new IllegalArgumentException("Unable to construct viewmodel");
    }
}
