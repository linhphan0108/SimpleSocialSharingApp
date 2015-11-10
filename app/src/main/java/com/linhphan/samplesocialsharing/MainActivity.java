package com.linhphan.samplesocialsharing;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.ShareApi;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.ShareOpenGraphObject;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    private static final String TWITTER_KEY = "sgWzwXFixQeeiMMpkBkyecbyF";
    private static final String TWITTER_SECRET = "bFrtU8qblD4MBxpEGHYsHyHALcLTB1VdZjeeFp0caIJTIT5D03";


    private final static int REQUEST_CODE_FACEBOOK = 11;
    private final static int REQUEST_CODE_TWITTER = 22;
    private final static int REQUEST_CODE_INSTAGRAM = 33;

    private Button mBtnFacebookSharing;
    private Button mBtnTwitterSharing;
    private Button mBtnInstagramSharing;

    //facebook
    private CallbackManager mFbCallbackManager;
    private LoginManager mLoginManager;

    //twitter
    private TwitterLoginButton loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        getAllWidgets();
        registerEventHandler();

        setupFacebook();
        setTwitter();

        retrieveHashKey();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri;
        switch (requestCode) {
            case REQUEST_CODE_FACEBOOK:
                uri = data.getData();
                shareToFacebook(uri);
                break;
            case REQUEST_CODE_TWITTER:
                uri = data.getData();
                shareToTwitter(uri);
                break;
            case REQUEST_CODE_INSTAGRAM:
                uri = data.getData();
                shareToInstagram(uri);
                break;
            default:
                break;
        }

        loginButton.onActivityResult(requestCode, resultCode, data);
        mFbCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_facebook_sharing:
                openGalleryApp(REQUEST_CODE_FACEBOOK);
                break;

            case R.id.btn_twitter_sharing:
                openGalleryApp(REQUEST_CODE_TWITTER);

                break;

            case R.id.btn_instagram_sharing:
                openGalleryApp(REQUEST_CODE_INSTAGRAM);

                break;

        }
    }

    private void getAllWidgets() {
        mBtnFacebookSharing = (Button) findViewById(R.id.btn_facebook_sharing);
        mBtnTwitterSharing = (Button) findViewById(R.id.btn_twitter_sharing);
        mBtnInstagramSharing = (Button) findViewById(R.id.btn_instagram_sharing);

        //twitter
        loginButton = (TwitterLoginButton) findViewById(R.id.twitter_login_button);
    }

    private void registerEventHandler() {
        mBtnFacebookSharing.setOnClickListener(this);
        mBtnTwitterSharing.setOnClickListener(this);
        mBtnInstagramSharing.setOnClickListener(this);
    }

    private void setTwitter() {
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig));
        Fabric.with(this, new TwitterCore(authConfig), new TweetComposer());
        loginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                // The TwitterSession is also available through:
                // Twitter.getInstance().core.getSessionManager().getActiveSession()
                TwitterSession session = result.data;
                // TODO: Remove toast and use the TwitterSession's userID
                // with your app's user model
                String msg = "@" + session.getUserName() + " logged in! (#" + session.getUserId() + ")";
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

            }

            @Override
            public void failure(TwitterException exception) {
                Log.d("TwitterKit", "Login with Twitter failure", exception);
            }
        });
    }

    private void setupFacebook() {
        FacebookSdk.sdkInitialize(getApplicationContext());
        mFbCallbackManager = CallbackManager.Factory.create();

    }

    /**
     * @see <a href="http://simpledeveloper.com/how-to-share-an-image-on-facebook-in-android/">references</a>
     * @param uri which locate the image
     */
    private void loginAndShareToFacebook(final Uri uri) {
        List<String> permissionNeeds = Arrays.asList("publish_actions");

        //this loginManager helps you eliminate adding a LoginButton to your UI
        mLoginManager = LoginManager.getInstance();
        mLoginManager.logInWithPublishPermissions(this, permissionNeeds);
        mLoginManager.registerCallback(mFbCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                sharePhotoToFacebook(uri);
            }

            @Override
            public void onCancel() {
                Log.e(getClass().getName(), "login is canceled");
            }

            @Override
            public void onError(FacebookException error) {
                Log.e(getClass().getName(), "login is error");
            }
        });
    }

    private void sharePhotoToFacebook(Uri uri) {
        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            SharePhoto photo = new SharePhoto.Builder()
                    .setBitmap(bitmap)
                    .setCaption("Give me my codez or I will ... you know, do that thing you don't like!")
                    .build();

            SharePhotoContent sharePhotoContent = new SharePhotoContent.Builder()
                    .addPhoto(photo)
                    .build();

            ShareDialog shareDialog = new ShareDialog(this);
            shareDialog.registerCallback(mFbCallbackManager, new FacebookCallback<Sharer.Result>() {
                @Override
                public void onSuccess(Sharer.Result result) {
                    Toast.makeText(getBaseContext(), "your image has been shared", Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onCancel() {
                    Toast.makeText(getBaseContext(), "sharing is canceled", Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onError(FacebookException error) {
                    Toast.makeText(getBaseContext(), "failed to share your image", Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                }
            });

            if (ShareDialog.canShow(SharePhotoContent.class))
                shareDialog.show(sharePhotoContent);
            else
                ShareApi.share(sharePhotoContent, new FacebookCallback<Sharer.Result>() {
                    @Override
                    public void onSuccess(Sharer.Result result) {
                        Toast.makeText(getBaseContext(), "your image has been shared", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(getBaseContext(), "sharing is canceled", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Toast.makeText(getBaseContext(), "failed to share your image", Toast.LENGTH_SHORT).show();
                        error.printStackTrace();
                    }
                });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void shareToFacebook(Uri uri) {
        Toast.makeText(this, "start sharing to facebook, " + uri.getPath(), Toast.LENGTH_SHORT).show();
        //com.facebook.orca
        //com.facebook.katana
        //com.example.facebook
        //com.facebook.android
        String facebookPackage = "com.facebook.katana";
        if (isAppInstalled(facebookPackage)) {
            //using default intent
//            Intent intent = new Intent(Intent.ACTION_SEND);
//            intent.setType("image/*");
//            intent.putExtra(Intent.EXTRA_STREAM, uri);
//            intent.putExtra(Intent.EXTRA_TEXT, "this is a simple extra text");
//            intent.setPackage(facebookPackage);
////            startActivity(Intent.createChooser(intent, "pick an app to ..."));
//            startActivity(intent);

            //using sdk but native facebook app has been installed
            sharePhotoToFacebook(uri);

        } else {
            loginAndShareToFacebook(uri);
        }


    }

    private void shareLinkToFacebook(String url) {
        ShareLinkContent content = new ShareLinkContent.Builder()
                .setContentUrl(Uri.parse(url))
                .setContentTitle("the content title")
                .setContentDescription("the content description")
                .setImageUrl(Uri.parse("http://24h-img.24hstatic.com/upload/4-2015/images/2015-11-10/1447132597-1447131753-nhu-thao-6179-1447055440.jpg"))
                .build();
        ShareDialog shareDialog = new ShareDialog(this);
        shareDialog.registerCallback(mFbCallbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                Toast.makeText(getBaseContext(), "share a link successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                Toast.makeText(getBaseContext(), "share a link is canceled", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(getBaseContext(), "share a link is failed", Toast.LENGTH_SHORT).show();
                error.printStackTrace();

            }
        });
        shareDialog.show(content);
    }

    private void shareToFacebookUsingOGSApi(Uri uri) {
        //create an open graph object
        ShareOpenGraphObject object = new ShareOpenGraphObject.Builder()
                .putString("og:type", "books.book")
                .putString("og:title", "A Game of Thrones")
                .putString("og:description", "In the frozen wastes to the north of Winterfell, " +
                        "sinister and supernatural forces are mustering.")
                .putString("books:isbn", "0-553-57340-3")
                .build();

        //create a photo
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tiger);
        SharePhoto sharePhoto = new SharePhoto.Builder()
                .setBitmap(bitmap)
                .setImageUrl(Uri.parse("http://www.gettyimages.co.uk/gi-resources/images/Homepage/Category-Creative/UK/UK_Creative_462809583.jpg"))
                .setCaption("the simple caption")
                .build();

        //create an action
        ShareOpenGraphAction action = new ShareOpenGraphAction.Builder()
                .setActionType("books.reads")
//                .putObject("book", object)
                .putPhoto("image", sharePhoto)
                .build();

        //create the content
        ShareOpenGraphContent content = new ShareOpenGraphContent.Builder()
                .setPreviewPropertyName("book")
                .setAction(action)
                .build();

        ShareDialog shareDialog = new ShareDialog(this);
        shareDialog.registerCallback(mFbCallbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                Toast.makeText(getBaseContext(), "share a link successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                Toast.makeText(getBaseContext(), "share a link is canceled", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(getBaseContext(), "share a link is failed", Toast.LENGTH_SHORT).show();
                error.printStackTrace();

            }
        });
        shareDialog.show(content);
    }

    /**
     * share an image to twitter using fabric
     * there a 3 variants way to do this purpose
     * the first way uses default intent
     * the second way uses Twitter Tweet Composer
     * the third way uses TwitterKit Tweet Composer
     *
     * @see <a href="https://docs.fabric.io/android/twitter/compose-tweets.html"> read more about fobric</a>
     */
    private void shareToTwitter(Uri uri) {
        Toast.makeText(this, "start sharing to twitter " + uri.getPath(), Toast.LENGTH_SHORT).show();
        String twitterPackage = "com.twitter.android";
        if (isAppInstalled(twitterPackage)) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_TEXT, "the simple text");
            intent.setType("image/*");
            intent.setPackage(twitterPackage);
            startActivity(intent);

            //using Twitter Tweet Composer
            //The Twitter Android Application allows apps to start the exported Tweet composer via an Intent.
            //The Twitter Android compose is feature-rich, familiar to users, and has options for attaching images and videos.
            //If the Twitter app is not installed, the intent will launch twitter.com in a browser, but the specified image will be ignored.
//            TweetComposer.Builder builder = new TweetComposer.Builder(this)
//                    .text("just setting up my Fabric.")
//                    .image(uri);
//            builder.show();

        } else {
            //The TwitterKit Tweet Composer (Beta) is a lightweight composer which lets users compose Tweets with App Cards from within your application.
            // It does not depend on the Twitter for Android app being installed.

//            Card card = new Card.AppCardBuilder(MainActivity.this)
//                    .imageUri(uri)
//                    .iPhoneId("123456")
//                    .iPadId("654321")
//                    .build();
//
//            TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();
//            Intent intent = new ComposerActivity.Builder(MainActivity.this).session(session).card(card).createIntent();
//            startActivity(intent);
        }
    }

    /**
     * share an image to instagram application
     * note: instagram don't allow sharing text
     *
     * @param uri which locate the image
     * @see <a href="https://instagram.com/developer/mobile-sharing/android-intents/">Instagram docs</a>
     */

    private void shareToInstagram(Uri uri) {
        Toast.makeText(this, "start sharing to instagram " + uri.getPath(), Toast.LENGTH_SHORT).show();
        String instagramPackage = "com.instagram.android";
        if (isAppInstalled(instagramPackage)) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setPackage(instagramPackage);
//            startActivity(Intent.createChooser(intent, "Share to"));
            startActivity(intent);
        } else {
            Toast.makeText(this, "to use this feature, you must have an Instagram application is installed", Toast.LENGTH_SHORT).show();
        }

    }

    private void openGalleryApp(int requestCode) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, requestCode);
    }

    /**
     * determine whether an application is installed or not
     *
     * @param packageName the full package name
     * @return true if the application is installed whereas return false
     */
    private boolean isAppInstalled(String packageName) {
        boolean isInstalled = false;

        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(packageName, 0);
            isInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return isInstalled;
    }

    private void retrieveHashKey() {
        // Add code to print out the key hash
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.linhphan.samplesocialsharing",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }
    }


}
