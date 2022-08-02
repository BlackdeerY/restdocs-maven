package io.blackdeer.restdocsmaven;

import io.blackdeer.restdocsmaven.device.Device;
import io.blackdeer.restdocsmaven.device.DeviceRepository;
import io.blackdeer.restdocsmaven.hub.Hub;
import io.blackdeer.restdocsmaven.hub.HubRepository;
import io.blackdeer.restdocsmaven.user.User;
import io.blackdeer.restdocsmaven.user.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith({SpringExtension.class, RestDocumentationExtension.class})
@SpringBootTest
//@AutoConfigureMockMvc    // .webAppContextSetup(webApplicationContext)
//@AutoConfigureRestDocs    // .apply(documentationConfiguration(restDocumentationContextProvider))
public class RescdocsTest {

    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private HubRepository hubRepository;

    @MockBean
    private DeviceRepository deviceRepository;

    private static ArrayList<User> users;
    private static ArrayList<Hub> hubs;
    private static ArrayList<Device> devices;

    @BeforeEach
    public void beforeEach(WebApplicationContext webApplicationContext,
                           RestDocumentationContextProvider restDocumentationContextProvider) {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(new CharacterEncodingFilter("UTF-8", true))
                .apply(documentationConfiguration(restDocumentationContextProvider)
                        .uris()
                        .withScheme("https")
                        .withHost("example.io")
                        .withPort(8084)
                )
//                .alwaysDo(document("{method-name}/{step}"))
                .build();
    }

    @BeforeAll
    public static void beforeAll() {
        users = new ArrayList<>(10);
        for (int i = 0; i < 10; ++i) {
            User user = new User(Long.valueOf(i + 1), String.format("사용자%02d", i + 1));
            users.add(user);
        }
        hubs = new ArrayList<>(20);
        devices = new ArrayList<>(100);
        for (int i = 0; i < 20; ++i) {
            User user = users.get(i / 2);
            Hub hub = new Hub(Long.valueOf(i + 1), String.format("%s의 %s허브", user.getName(), i % 2 == 0 ? "거실" : "별장"));
            int deviceCount = i % 2 + 2;
            for (int j = 0; j < deviceCount; ++j) {
                String deviceName = null;
                if (j == 0) {
                    deviceName = "침실전등";
                } else if (j == 1) {
                    deviceName = "화장실전등";
                } else {
                    deviceName = "차고전등";
                }
                Device device = new Device(Long.valueOf(devices.size() + 1), deviceName, hub);
                device.setHub(hub);
                hub.getDevices().add(device);
                devices.add(device);
            }
            hub.getUsers().add(user);
            user.getHubs().add(hub);
            hubs.add(hub);
        }
    }

    @Test
    public void restdocs() throws Exception {
        for (User user : users) {
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        }
        for (Hub hub : hubs) {
            when(hubRepository.findById(hub.getId())).thenReturn(Optional.of(hub));
        }
        for (Device device : devices) {
            when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));
        }
        when(userRepository.findAll()).thenReturn(users);
        when(hubRepository.findAll()).thenReturn(hubs);
        when(deviceRepository.findAll()).thenReturn(devices);

        FieldDescriptor[] fieldDescriptorsUser = new FieldDescriptor[] {
                fieldWithPath("id")
                        .description("사용자 식별 ID"),
                fieldWithPath("name")
                        .description("사용자가 입력한 사용자 이름"),
                subsectionWithPath("hubs")
                        .description("사용자가 권한을 가진 허브들")
        };

        FieldDescriptor[] fieldDescriptorsHub = new FieldDescriptor[] {
                fieldWithPath("id")
                        .description("허브 식별 ID"),
                fieldWithPath("name")
                        .description("사용자가 지정한 허브의 이름"),
                subsectionWithPath("devices")
                        .description("허브에 연결된 장치들")
        };

        FieldDescriptor[] fieldDescriptorsDevice = new FieldDescriptor[] {
                fieldWithPath("id")
                        .description("장치 식별 ID"),
                fieldWithPath("name")
                        .description("사용자가 지정한 장치의 이름")
        };

        this.mockMvc.perform(
                        RestDocumentationRequestBuilders
                                .get(
                                        "/hub/get/{id}",
                                        1L
                                )
                                .header("Authorization", "Bearer **********...")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("test",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                )
        );

        this.mockMvc.perform(
                RestDocumentationRequestBuilders
                        .get(
                                "/user/get/{id}",
                                1L
                        )
                        .header("Authorization", "Bearer **********...")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "get-user-by-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization")
                                                .description("로그인한 계정의 권한 사용")
                                ),
                                pathParameters(
                                        parameterWithName("id")
                                                .description("사용자 식별 ID")
                                ),
                                responseFields(
                                        fieldDescriptorsUser
                                )
                        )
                );
        this.mockMvc.perform(
                RestDocumentationRequestBuilders
                        .get(
                                "/hub/get/{id}",
                                1L
                        )
                        .header("Authorization", "Bearer **********...")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "get-hub-by-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization")
                                                .description("로그인한 계정의 권한 사용")
                                ),
                                pathParameters(
                                        parameterWithName("id")
                                                .description("허브 식별 ID")
                                ),
                                responseFields(
                                        fieldDescriptorsHub
                                )

                        )
                );
        this.mockMvc.perform(
                        RestDocumentationRequestBuilders
                                .get(
                                        "/device/get/{id}",
                                        1L
                                )
                                .header("Authorization", "Bearer **********...")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "get-device-by-id",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization")
                                                .description("로그인한 계정의 권한 사용")
                                ),
                                pathParameters(
                                        parameterWithName("id")
                                                .description("장치 식별 ID")
                                ),
                                responseFields(
                                        fieldDescriptorsDevice
                                )

                        )
                );

        this.mockMvc.perform(
                        RestDocumentationRequestBuilders
                                .get(
                                        "/user/list"
                                )
                                .header("Authorization", "Bearer **********...")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "get-users-all",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization")
                                                .description("로그인한 계정의 권한 사용")
                                ),
                                responseFields(
                                        fieldWithPath("[]")
                                                .description("모든 사용자")
                                ).andWithPrefix("[].", fieldDescriptorsUser)
                        )
                );
        this.mockMvc.perform(
                        RestDocumentationRequestBuilders
                                .get(
                                        "/hub/list"
                                )
                                .header("Authorization", "Bearer **********...")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "get-hubs-all",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization")
                                                .description("로그인한 계정의 권한 사용")
                                ),
                                responseFields(
                                        fieldWithPath("[]")
                                                .description("모든 허브")
                                ).andWithPrefix("[].", fieldDescriptorsHub)
                        )
                );
        this.mockMvc.perform(
                        RestDocumentationRequestBuilders
                                .get(
                                        "/device/list"
                                )
                                .header("Authorization", "Bearer **********...")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "get-devices-all",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        headerWithName("Authorization")
                                                .description("로그인한 계정의 권한 사용")
                                ),
                                responseFields(
                                        fieldWithPath("[]")
                                                .description("모든 장치")
                                ).andWithPrefix("[].", fieldDescriptorsDevice)
                        )
                );
    }
}
