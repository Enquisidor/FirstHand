import { useEffect, useState } from 'react';
import {
  Container,
  Title,
  Text,
  Paper,
  Badge,
  Loader,
  Stack,
  Group,
  ThemeIcon,
  List,
} from '@mantine/core';
import { IconLeaf, IconCheck, IconAlertCircle } from '@tabler/icons-react';
import api from '@/lib/api';
import _ from 'lodash';

export default function Home() {
  const [status, setStatus] = useState<string>('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStatus = async () => {
      try {
        const response = await api.get('/status');
        setStatus(_.get(response, 'data.status', 'unknown'));
      } catch (error) {
        console.error('Failed to fetch status:', error);
        setStatus('error');
      } finally {
        setLoading(false);
      }
    };

    fetchStatus();
  }, []);

  return (
    <Container size="lg" py="xl">
      <Stack gap="xl">
        <Group justify="space-between" align="center">
          <Group>
            <ThemeIcon size="xl" radius="md" variant="gradient" gradient={{ from: 'green', to: 'lime' }}>
              <IconLeaf size={28} />
            </ThemeIcon>
            <div>
              <Title order={1}>FirstHand</Title>
              <Text c="dimmed" size="sm">
                Reducing food waste through regulatory compliance and smart coordination
              </Text>
            </div>
          </Group>
        </Group>

        <Paper shadow="sm" p="xl" radius="md" withBorder>
          <Stack gap="md">
            <Group justify="space-between">
              <Title order={3}>System Status</Title>
              {loading ? (
                <Loader size="sm" />
              ) : status === 'running' ? (
                <Badge color="green" leftSection={<IconCheck size={14} />}>
                  Online
                </Badge>
              ) : (
                <Badge color="red" leftSection={<IconAlertCircle size={14} />}>
                  {status === 'error' ? 'Offline' : status}
                </Badge>
              )}
            </Group>
            <Text c="dimmed">
              Backend API is {loading ? 'connecting...' : status === 'running' ? 'running and ready' : 'unavailable'}
            </Text>
          </Stack>
        </Paper>

        <Paper shadow="sm" p="xl" radius="md" withBorder>
          <Stack gap="lg">
            <Title order={3}>About FirstHand</Title>
            <Text>
              FirstHand helps Ontario&apos;s small food producers, co-ops, and independent retailers reduce waste
              by connecting surplus food with nearby buyers.
            </Text>

            <div>
              <Title order={4} mb="md">Two-Step Strategy</Title>
              <Stack gap="lg">
                <div>
                  <Text fw={600} mb="xs">Step 1: Build Trust Through Regulatory Friction Reduction</Text>
                  <List spacing="xs" size="sm">
                    <List.Item>Navigate Ontario&apos;s food safety regulations</List.Item>
                    <List.Item>Compliance checklists and documentation templates</List.Item>
                    <List.Item>Connect with nearby buyers</List.Item>
                    <List.Item>Coordinate logistics and pickup windows</List.Item>
                  </List>
                </div>

                <div>
                  <Text fw={600} mb="xs">Step 2: Layer On Waste-Reduction Routing</Text>
                  <List spacing="xs" size="sm">
                    <List.Item>Treat food waste as a routing optimization problem</List.Item>
                    <List.Item>Match surplus food with nearby buyers</List.Item>
                    <List.Item>Optimize multi-stop pickup routes</List.Item>
                  </List>
                </div>
              </Stack>
            </div>
          </Stack>
        </Paper>
      </Stack>
    </Container>
  );
}
