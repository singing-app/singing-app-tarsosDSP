package com.example.a220523.ui.pitch;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PitchViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public PitchViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Pitch fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}