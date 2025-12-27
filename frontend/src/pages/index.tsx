import { useEffect, useState } from 'react';
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
    <div style={{ padding: '2rem', fontFamily: 'system-ui, sans-serif' }}>
      <h1>FirstHand</h1>
      <p>Helping Ontario food producers with regulatory compliance and waste reduction.</p>

      <div style={{ marginTop: '2rem', padding: '1rem', backgroundColor: '#f5f5f5', borderRadius: '8px' }}>
        <h2>API Status</h2>
        {loading ? (
          <p>Loading...</p>
        ) : (
          <p>Backend Status: <strong>{status}</strong></p>
        )}
      </div>
    </div>
  );
}
