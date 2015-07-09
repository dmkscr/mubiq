package com.android.zigzag.mubi;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.ConfirmationActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

public class SendMessageToMobile extends Activity {

    private String                          TAG                            = "Mubiq";

    private final String                    MESSAGE_PATH                   = "/message";

    private EditText                        receivedMessagesEditText;
    private View                            messageButton;
    private GoogleApiClient                 mGoogleApiClient;
    private NodeApi.NodeListener            nodeListener;
    private String                          remoteNodeId;
    private MessageApi.MessageListener      messageListener;
    private Handler                         handler;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rect_activity_main);

        handler = new Handler();

        receivedMessagesEditText = (EditText) findViewById(R.id.receivedMessagesEditText);
        messageButton = findViewById(R.id.messageButton);

        // Set messageButton onClickListener to send message
        messageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Wearable.MessageApi.sendMessage(mGoogleApiClient, remoteNodeId, MESSAGE_PATH, null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
//                        Log.d(TAG, "sendMessageResult " + sendMessageResult);
                        Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
                        if (sendMessageResult.getStatus().isSuccess()) {
//                            Log.d(TAG, "sendMessageResult status (success): " + sendMessageResult.getStatus().isSuccess());
//                            intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
//                            intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.message_sent));
                        } else {
//                            Log.d(TAG, "sendMessageResult status (failure): " + sendMessageResult.getStatus().isSuccess());
//                            intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
//                            intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.error_message));
                        }
//                        startActivity(intent);
                    }
                });
            }
        });

        // Create NodeListener that enables buttons when a node is connected and disables buttons when a node is disconnected
        nodeListener = new NodeApi.NodeListener() {
            @Override
            public void onPeerConnected(Node node) {
                remoteNodeId = node.getId();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // tbd
                    }
                });
                Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
//                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
//                intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.peer_connected));
                startActivity(intent);
            }

            @Override
            public void onPeerDisconnected(Node node) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // tbd
                    }
                });
                Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
                startActivity(intent);
            }
        };



        // Create MessageListener that receives messages sent from mobile
        messageListener = new MessageApi.MessageListener() {
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {
                if (messageEvent.getPath().equals(MESSAGE_PATH)) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // tbd
                        }
                    });
                }
            }
        };

        // Create GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext()).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                Wearable.NodeApi.addListener(mGoogleApiClient, nodeListener);
                Wearable.MessageApi.addListener(mGoogleApiClient, messageListener);
                // If there is a connected node, get it's id that is used when sending messages
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                        if (getConnectedNodesResult.getStatus().isSuccess() && getConnectedNodesResult.getNodes().size() > 0) {
                            List<Node> foundNodes = getConnectedNodesResult.getNodes();
                            Log.d(TAG, "foundNodes: " + getConnectedNodesResult.getNodes());
                            for (int i=0; i<foundNodes.size(); i++) {
                                if( foundNodes.get(i).getId() != "cloud" && foundNodes.get(i).isNearby() == true ) {
                                    Log.d(TAG, "Node: " + foundNodes.get(i));
                                    remoteNodeId = foundNodes.get(i).getId();
                                    Log.d(TAG, "Id: " + remoteNodeId);
                                }
                            }
                        }
                    }
                });
            }

            @Override
            public void onConnectionSuspended(int i) {
                messageButton.setEnabled(false);
            }
        }).addApi(Wearable.API).build();

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check is Google Play Services available
        int connectionResult = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());

        if (connectionResult != ConnectionResult.SUCCESS) {
            // Google Play Services is NOT available. Show appropriate error dialog
            GooglePlayServicesUtil.showErrorDialogFragment(connectionResult, this, 0, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
        } else {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onPause() {
        Wearable.NodeApi.removeListener(mGoogleApiClient, nodeListener);
        Wearable.MessageApi.removeListener(mGoogleApiClient, messageListener);
        mGoogleApiClient.disconnect();
        super.onPause();
    }
}