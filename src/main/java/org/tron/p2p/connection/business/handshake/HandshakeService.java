package org.tron.p2p.connection.business.handshake;

import lombok.extern.slf4j.Slf4j;
import org.tron.p2p.base.Parameter;
import org.tron.p2p.connection.Channel;
import org.tron.p2p.connection.ChannelManager;
import org.tron.p2p.connection.business.MessageProcess;
import org.tron.p2p.connection.message.Message;
import org.tron.p2p.connection.message.handshake.HelloMessage;

@Slf4j(topic = "net")
public class HandshakeService implements MessageProcess {

  private final int version = Parameter.p2pConfig.getVersion();

  public void startHandshake(Channel channel) {
    sendHelloMsg(channel, DisconnectCode.NORMAL);
  }

  @Override
  public void processMessage(Channel channel, Message message) {
    log.info("Receive HelloMessage from {}:{}", channel.getInetAddress(),
        channel.getInetSocketAddress().getPort());
    HelloMessage msg = (HelloMessage) message;

    if (channel.isFinishHandshake()) {
      channel.close();
      log.warn("Close channel {}, handshake is finished", channel.getInetAddress());
      return;
    }

    channel.setFinishHandshake(true);

    if (channel.isActive()) {
      if (msg.getCode() != DisconnectCode.NORMAL.getValue()
          || msg.getVersion() != version) {
        log.info("Handshake failed {}, code: {}, version: {}",
                channel.getInetAddress(),
                DisconnectCode.NORMAL.getValue(),
                msg.getVersion());
        channel.close();
      }
      //if channel is already active, wo do nothing
      return;
    }

    if (msg.getVersion() != version) {
      log.info("Peer {} different p2p version, peer->{}, me->{}",
          channel.getInetAddress(), msg.getVersion(), version);
      sendHelloMsg(channel, DisconnectCode.DIFFERENT_VERSION);
      channel.close();
      return;
    }

    DisconnectCode code = ChannelManager.processPeer(channel);
    if (code != DisconnectCode.NORMAL) {
      sendHelloMsg(channel, code);
      channel.close();
      return;
    }

    sendHelloMsg(channel, DisconnectCode.NORMAL);
  }

  private void sendHelloMsg(Channel channel, DisconnectCode code) {
    HelloMessage helloMessage = new HelloMessage(code);
    channel.send(helloMessage);
    log.info("Handshake send to {}:{}, {} ", channel.getInetAddress(),
        channel.getInetSocketAddress().getPort(), helloMessage);
  }

}
