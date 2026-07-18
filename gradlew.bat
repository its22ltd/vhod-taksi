<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textStyle="bold"
            android:textSize="18sp"
            android:text="Апартаменти" />

        <LinearLayout
            android:id="@+id/containerApts"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnAddApt"
            style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAllCaps="false"
            android:text="+ Добави апартамент" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="20dp"
            android:textStyle="bold"
            android:textSize="18sp"
            android:text="Разходи" />

        <LinearLayout
            android:id="@+id/containerExps"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnAddExp"
            style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAllCaps="false"
            android:text="+ Добави разход" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="20dp"
            android:textStyle="bold"
            android:textSize="18sp"
            android:text="Приходи" />

        <LinearLayout
            android:id="@+id/containerIncs"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnAddInc"
            style="@style/Widget.MaterialComponents.Button.OutlinedButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAllCaps="false"
            android:text="+ Добави приход" />

        <TextView
            android:id="@+id/tvPreview"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:padding="10dp"
            android:background="#11000000"
            android:textSize="13sp" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnSaveData"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:textAllCaps="false"
            android:text="Запази" />
    </LinearLayout>
</ScrollView>
