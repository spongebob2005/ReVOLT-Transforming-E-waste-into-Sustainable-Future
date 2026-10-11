package com.example.revolt.data;

import com.example.revolt.data.model.AuthResponse;
import com.example.revolt.data.model.Organization;
import com.example.revolt.data.model.PickupRequest;
import com.example.revolt.data.model.Stats;
import com.example.revolt.data.model.User;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;

/** REST endpoints served by the Node backend in /backend. */
public interface ApiService {

    @GET("api/health")
    Call<Map<String, Object>> health();

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body Map<String, String> body);

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body Map<String, String> body);

    @GET("api/auth/me")
    Call<User> me();

    @GET("api/organizations")
    Call<List<Organization>> organizations();

    @GET("api/requests")
    Call<List<PickupRequest>> myRequests();

    @GET("api/requests/stats")
    Call<Stats> myStats();

    @GET("api/requests/{id}")
    Call<PickupRequest> request(@Path("id") long id);

    @POST("api/requests/{id}/cancel")
    Call<PickupRequest> cancelRequest(@Path("id") long id);

    /** {@code image} may be null, in which case the request is sent without a photo. */
    @Multipart
    @POST("api/requests")
    Call<PickupRequest> createRequest(@PartMap Map<String, RequestBody> fields,
                                      @Part MultipartBody.Part image);
}
