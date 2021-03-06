package com.grohden.niceanimals.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import com.grohden.niceanimals.R
import com.grohden.niceanimals.realm.entities.NiceAnimal
import com.grohden.niceanimals.services.NiceAnimalsService
import com.grohden.niceanimals.ui.base.BaseActivity
import dagger.android.AndroidInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import java.util.*
import javax.inject.Inject
import android.util.Pair as UtilPair

/**
 * SplashScreen activity is responsible for initializing the app
 * initial data, it should at least wait 2s before go to another activity
 */
class SplashScreenActivity : BaseActivity() {

    @Inject
    lateinit var realm: Realm

    @Inject
    lateinit var niceAnimalsService: NiceAnimalsService

    private fun findFirstAnimal(): NiceAnimal? {
        return realm
                .where<NiceAnimal>(NiceAnimal::class.java)
                .findFirst()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash_screen)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        if (findFirstAnimal() != null) {
            Handler().postDelayed(
                    { this.goToMainScreen() },
                    DEFAULT_SCREEN_TIME.toLong()
            )
        } else {
            handleEmptyDBInitialization()
        }
    }

    /**
     * Handles app first initialization (actually, it is called only when
     * it doesn't find any animal in realm DB)
     */
    private fun handleEmptyDBInitialization() {
        niceAnimalsService
                .fetchAndPersistAllTypes()
                .subscribe { _, ex ->
                    if (ex != null) {
                        Toast.makeText(applicationContext, "Exception trying to get animals :/", Toast.LENGTH_LONG).show()
                        Log.e("SplashScreenActivity", "Error at empty DB initialization", ex)
                    } else {
                        goToMainScreen()
                    }
                }
    }


    private fun goToMainScreen() {
        val intent = Intent(
                this,
                MainActivity::class.java
        )

        startActivity(intent)
        finish()
    }

    companion object {
        private const val DEFAULT_SCREEN_TIME = 1000
    }
}
